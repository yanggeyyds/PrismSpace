package com.yzddmr6.prismspace.prism.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing

// 禁用态统一弱化透明度（M3 onSurface disabled 约定）。
// Internal so SpaceScreen AppCard and other Compose files in this module can share the same value.
internal const val DisabledAlpha = 0.38f

/** Single explicit leading-icon size token shared by row components. */
internal val PrismIconSize = PrismSpacing.Xl

// ────────────────────────────────────────────────────────────────
//  Shared private layout shell — exposes RowScope
// ────────────────────────────────────────────────────────────────
@Composable
private fun RowShell(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .padding(horizontal = PrismSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun TitleAndSummary(
    title: String,
    summary: String?,
    muted: Boolean = false,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val titleColor = when {
        muted -> MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
        danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val summaryColor =
        if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DisabledAlpha)
        else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = summaryColor,
            )
        }
    }
}

@Composable
private fun RowLeadingIcon(icon: ImageVector, enabled: Boolean = true, danger: Boolean = false) {
    Box(
        modifier = Modifier.padding(end = PrismSpacing.Lg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(PrismIconSize),
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                danger -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

// ────────────────────────────────────────────────────────────────
//  1. ActionRow — one-shot action, entire row clickable
// ────────────────────────────────────────────────────────────────
@Composable
fun ActionRow(
    title: String,
    summary: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    RowShell(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        if (leadingIcon != null) {
            RowLeadingIcon(leadingIcon, enabled, danger)
        }
        TitleAndSummary(
            title = title,
            summary = summary,
            muted = !enabled,
            danger = danger,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = PrismSpacing.Sm),
        )
        if (trailing != null) {
            trailing()
        }
    }
}

// ────────────────────────────────────────────────────────────────
//  2. SwitchRow — persistent toggle, entire row toggles
// ────────────────────────────────────────────────────────────────
@Composable
fun SwitchRow(
    title: String,
    summary: String? = null,
    leadingIcon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    disabledReason: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val effectiveSummary = if (!enabled && disabledReason != null) disabledReason else summary

    RowShell(
        modifier = Modifier.clickable(enabled = enabled, onClick = { onCheckedChange(!checked) }),
    ) {
        if (leadingIcon != null) {
            RowLeadingIcon(leadingIcon, enabled)
        }
        TitleAndSummary(
            title = title,
            summary = effectiveSummary,
            muted = !enabled,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = PrismSpacing.Sm),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.padding(start = PrismSpacing.Lg),
        )
    }
}

// ────────────────────────────────────────────────────────────────
//  3. NavRow — navigation, fixed arrow trailing
// ────────────────────────────────────────────────────────────────
@Composable
fun NavRow(
    title: String,
    summary: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
) {
    RowShell(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        if (leadingIcon != null) {
            RowLeadingIcon(leadingIcon)
        }
        TitleAndSummary(
            title = title,
            summary = summary,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = PrismSpacing.Sm),
        )
        Icon(
            imageVector = PrismIcons.Chev,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = PrismSpacing.Sm),
        )
    }
}

// ────────────────────────────────────────────────────────────────
//  4. StatusRow — read-only; NO clickable, NO ripple
// ────────────────────────────────────────────────────────────────
@Composable
fun StatusRow(
    title: String,
    value: String? = null,
    summary: String? = null,
    statusTag: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
) {
    RowShell {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(PrismIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(PrismSpacing.Lg))
        }
        // Two modes:
        //  • summary != null  → title + secondary line STACKED below (like ActionRow). Use for long
        //    text (a version sentence, a file's location/time) so a long string never squeezes the
        //    title into a narrow, multi-wrapping column.
        //  • value  != null   → short label : value pair (value on the trailing side).
        TitleAndSummary(
            title = title,
            summary = summary,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = PrismSpacing.Sm),
        )
        if (summary == null && value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = PrismSpacing.Sm),
            )
        } else if (summary == null && statusTag != null) {
            statusTag()
        }
    }
}
