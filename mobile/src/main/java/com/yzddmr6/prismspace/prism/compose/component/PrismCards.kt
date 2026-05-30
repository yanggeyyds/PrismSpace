package com.yzddmr6.prismspace.prism.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yzddmr6.prismspace.prism.compose.theme.LocalPrismExtraColors
import com.yzddmr6.prismspace.prism.compose.theme.PrismRadius
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing

// ────────────────────────────────────────────────────────────────
//  StatusTag — small rounded label
// ────────────────────────────────────────────────────────────────
@Composable
fun StatusTag(text: String, level: PrismLevel) {
    val extra = LocalPrismExtraColors.current
    val background = when (level) {
        PrismLevel.Ok    -> extra.okContainer
        PrismLevel.Warn  -> extra.warnContainer
        PrismLevel.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (level) {
        PrismLevel.Ok    -> extra.ok
        PrismLevel.Warn  -> extra.warn
        PrismLevel.Error -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = CircleShape,
        color = background,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = PrismSpacing.Sm, vertical = PrismSpacing.Xs),
        )
    }
}

// ────────────────────────────────────────────────────────────────
//  StatusHeroCard — prominent tonal card
// ────────────────────────────────────────────────────────────────
@Composable
fun StatusHeroCard(
    level: PrismLevel,
    title: String,
    body: String,
    tag: String,
    leadingIcon: ImageVector,
    primary: (@Composable () -> Unit)? = null,
) {
    val extra = LocalPrismExtraColors.current
    val containerColor = when (level) {
        PrismLevel.Ok    -> extra.okContainer
        PrismLevel.Warn  -> extra.warnContainer
        PrismLevel.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (level) {
        PrismLevel.Ok    -> extra.ok
        PrismLevel.Warn  -> extra.warn
        PrismLevel.Error -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PrismRadius.Lg),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(PrismSpacing.Lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = PrismSpacing.Md),
                    tint = contentColor,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                    )
                }
                Box(modifier = Modifier.padding(start = PrismSpacing.Sm)) {
                    StatusTag(text = tag, level = level)
                }
            }
            if (primary != null) {
                Box(modifier = Modifier.padding(top = PrismSpacing.Md)) {
                    primary()
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
//  StatCard — small secondary card
// ────────────────────────────────────────────────────────────────
/**
 * Renders value prominently on top, label below — param order is (label, value).
 */
@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PrismRadius.Lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LocalPrismExtraColors.current.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(PrismSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(PrismSpacing.Xs),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
//  GroupCard — SegmentedColumn style
//  M3 1.1.2 adaptation: surfaceContainerHighest not available;
//  using surfaceVariant (closest semantic equivalent).
// ────────────────────────────────────────────────────────────────
@Composable
fun GroupCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = PrismSpacing.Lg)
                    .padding(bottom = PrismSpacing.Xs),
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PrismRadius.Lg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, LocalPrismExtraColors.current.cardBorder),
        ) {
            Column(
                // 8/4 rhythm (was a too-tight 4/2 that made grouped rows feel cramped).
                modifier = Modifier.padding(vertical = PrismSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(PrismSpacing.Xs),
                content = content,
            )
        }
    }
}
