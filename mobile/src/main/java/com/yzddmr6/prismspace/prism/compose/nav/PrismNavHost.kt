package com.yzddmr6.prismspace.prism.compose.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yzddmr6.prismspace.prism.compose.screen.FilesScreen
import com.yzddmr6.prismspace.prism.compose.screen.HomeScreen
import com.yzddmr6.prismspace.prism.compose.screen.SettingsScreen
import com.yzddmr6.prismspace.prism.compose.screen.SpaceScreen
import com.yzddmr6.prismspace.prism.compose.vm.AppFeedbackBus

/**
 * The ONE canonical "switch to a top-level tab" operation. Every entry point (the bottom bar AND any
 * in-screen CTA like Home's "克隆一个应用") must go through this — a bare navigate(route) from a screen
 * pushes a duplicate destination outside the saved-state graph and strands the start tab (the 首页-stuck
 * bug). launchSingleTop + popUpTo(start){saveState} + restoreState is the standard tab pattern.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.findStartDestination().id) { saveState = true }
        restoreState = true
    }
}

@Composable
fun PrismNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Hide the 4-tab nav while the Space screen is in multi-select (its batch bar owns the screen).
    val multiSelectActive by AppLaunchSignals.multiSelectActive.collectAsState()
    val showBottomBar = currentRoute in PrismRoutes.topLevel && !multiSelectActive

    val snackbarHostState = remember { SnackbarHostState() }
    val feedback by AppFeedbackBus.feedback.collectAsState()
    LaunchedEffect(feedback) {
        val f = feedback
        if (f != null) {
            if (f.message.isNotBlank()) snackbarHostState.showSnackbar(f.message)
            AppFeedbackBus.consume()
        }
    }
    LaunchedEffect(navController) {
        AppLaunchSignals.resetToHome.collect {
            navController.navigate(PrismRoutes.HOME) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
    LaunchedEffect(navController) {
        // "去启用" from the clone install-method selector → jump to Settings (run-mode row is at the top).
        AppLaunchSignals.openRunMode.collect {
            navController.navigate(PrismRoutes.SETTINGS) {
                launchSingleTop = true
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                restoreState = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                PrismBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route -> navController.navigateToTab(route) },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val isError = feedback?.isError == true
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                   else MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController  = navController,
                startDestination = PrismRoutes.HOME,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(PrismRoutes.HOME)     { HomeScreen(navController) }
                composable(PrismRoutes.SPACE)    { SpaceScreen(navController) }
                composable(PrismRoutes.FILES)    { FilesScreen(navController) }
                composable(PrismRoutes.SETTINGS) { SettingsScreen(navController) }
            }
        }
    }
}
