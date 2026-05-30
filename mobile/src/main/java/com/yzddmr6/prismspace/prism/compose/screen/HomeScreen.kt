package com.yzddmr6.prismspace.prism.compose.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.component.GroupCard
import com.yzddmr6.prismspace.prism.compose.component.PrismIcons
import com.yzddmr6.prismspace.prism.compose.component.PrismLevel
import com.yzddmr6.prismspace.prism.compose.component.StatCard
import com.yzddmr6.prismspace.prism.compose.component.StatusHeroCard
import com.yzddmr6.prismspace.prism.compose.component.StatusRow
import com.yzddmr6.prismspace.prism.compose.nav.PrismRoutes
import com.yzddmr6.prismspace.prism.compose.nav.navigateToTab
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing
import com.yzddmr6.prismspace.prism.compose.vm.ActionFeedback
import com.yzddmr6.prismspace.prism.compose.vm.AppFeedbackBus
import com.yzddmr6.prismspace.prism.compose.vm.HomePrimaryAction
import com.yzddmr6.prismspace.prism.compose.vm.HomeViewModel
import com.yzddmr6.prismspace.setup.SetupFlow

/** PrismSpace repository URL. */
private const val PRISM_GITHUB_URL = "https://github.com/yzddmr6/PrismSpace"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val vm: HomeViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.refresh() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lz_home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    // GitHub icon — normal web Intent (not app launch lifeline)
                    IconButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PRISM_GITHUB_URL))
                            )
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = "GitHub",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // 关于(Info) icon: show version via the unified Snackbar bus.
                    IconButton(onClick = {
                        val state = uiState
                        val msg = if (state != null) "PrismSpace ${state.versionName}" else "PrismSpace"
                        AppFeedbackBus.emit(ActionFeedback(msg, isError = false))
                    }) {
                        Icon(
                            imageVector = PrismIcons.Info,
                            contentDescription = stringResource(R.string.lz_home_about),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PrismSpacing.Lg, vertical = PrismSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(PrismSpacing.Md),
        ) {
            val state = uiState

            // ── Status Hero Card ─────────────────────────────────────────────
            StatusHeroCard(
                level       = state?.level ?: PrismLevel.Ok,
                title       = state?.statusTitle ?: stringResource(R.string.lz_home_loading),
                body        = state?.statusBody ?: "",
                tag         = state?.tag ?: "",
                // Status icon is always the space "shield"; severity is conveyed by the card's level
                // color/tag. (Was the wrench — an action icon — which is semantically wrong as a status.)
                leadingIcon = PrismIcons.Shield,
                primary     = when {
                    state?.showRepair == true -> {
                        {
                            Button(
                                onClick   = {
                                    when (state.primaryAction) {
                                        HomePrimaryAction.StartSetup -> SetupFlow.open(context)
                                        HomePrimaryAction.OpenSettings -> vm.repair { route -> nav.navigateToTab(route) }
                                        HomePrimaryAction.OpenSpace -> nav.navigateToTab(PrismRoutes.SPACE)
                                    }
                                },
                                modifier  = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = PrismIcons.Wrench,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = PrismSpacing.Sm),
                                )
                                // Label reflects the actual broken state (创建/恢复/修复), not always "修复".
                                Text(state.primaryLabel.ifBlank { stringResource(R.string.lz_home_repair_fallback) })
                            }
                        }
                    }
                    // Healthy state: give Home a real primary action instead of being
                    // a dead-end status screen.
                    state != null -> {
                        {
                            Button(
                                onClick  = { nav.navigateToTab(PrismRoutes.SPACE) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = PrismIcons.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = PrismSpacing.Sm),
                                )
                                Text(stringResource(R.string.lz_home_label_add_app))
                            }
                        }
                    }
                    else -> null
                },
            )

            // ── Stat Cards row: 主空间 / 双开空间 ────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PrismSpacing.Md),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val mainValue = state?.mainCount?.let {
                        stringResource(R.string.lz_home_main_count, it)
                    } ?: "--"
                    StatCard(label = stringResource(R.string.lz_home_main_space), value = mainValue)
                }
                Box(modifier = Modifier.weight(1f)) {
                    val cloneValue = state?.cloneCount?.let {
                        stringResource(R.string.lz_home_clone_count, it)
                    } ?: "--"
                    StatCard(label = stringResource(R.string.lz_home_dual_space), value = cloneValue)
                }
            }

            // ── Info GroupCard — no section title ─────────────────────────────
            // (PrismSpace 版本 row removed — version lives in 设置 → 关于; it was shown in 4 places.)
            GroupCard(title = null) {
                // Row: 工作资料 (Profile Owner)
                StatusRow(
                    title = stringResource(R.string.lz_home_work_profile),
                    value = state?.profileOwnerLabel ?: "…",
                    leadingIcon = PrismIcons.Shield,
                )
                // Row: 运行模式 (same label as Settings — single name for one concept)
                StatusRow(
                    title = stringResource(R.string.lz_home_run_mode),
                    value = state?.capabilityText ?: "…",
                    leadingIcon = PrismIcons.Key,
                )
                // Row: Android 版本
                StatusRow(
                    title = stringResource(R.string.lz_home_android_version),
                    value = state?.androidText ?: "…",
                    leadingIcon = PrismIcons.Droid,
                )
                // Row 5: 设备
                StatusRow(
                    title = stringResource(R.string.lz_home_device),
                    value = state?.deviceText ?: "…",
                    leadingIcon = PrismIcons.Phone,
                )
            }

            Spacer(Modifier.height(PrismSpacing.Sm))
        }
    }
}
