package com.yzddmr6.prismspace.prism.compose.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.component.PrismIcons
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing
import androidx.compose.ui.Modifier

private data class BottomNavItem(
    val labelRes: Int,
    val route: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    BottomNavItem(R.string.lz_nav_home,     PrismRoutes.HOME,     PrismIcons.Home),
    BottomNavItem(R.string.lz_nav_space,    PrismRoutes.SPACE,    PrismIcons.Grid),
    BottomNavItem(R.string.lz_nav_files,    PrismRoutes.FILES,    PrismIcons.Files),
    BottomNavItem(R.string.lz_nav_settings, PrismRoutes.SETTINGS, PrismIcons.Settings),
)

@Composable
fun PrismBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Column {
        androidx.compose.material.Divider(
            thickness = PrismSpacing.Hair,
            color = MaterialTheme.colorScheme.outline,
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected     = selected,
                onClick      = { onNavigate(item.route) },
                icon         = {
                    val label = stringResource(item.labelRes)
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = null,
                        modifier           = Modifier.semantics { contentDescription = label },
                    )
                },
                // Text labels (was icon-only) — discoverability: a 4-tab bar should name its tabs.
                label          = { Text(stringResource(item.labelRes), style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
        }
    }
}
