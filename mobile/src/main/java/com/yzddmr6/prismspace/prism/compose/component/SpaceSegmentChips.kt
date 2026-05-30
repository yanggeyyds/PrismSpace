package com.yzddmr6.prismspace.prism.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.space.SpaceChip
import com.yzddmr6.prismspace.prism.compose.theme.LocalPrismExtraColors
import com.yzddmr6.prismspace.prism.compose.theme.PrismRadius
import com.yzddmr6.prismspace.prism.compose.theme.PrismSpacing

/**
 * Segmented control for the scrollable space switcher. Selected chips use a
 * surface fill and subtle shadow; unselected/create chips stay transparent.
 */
@Composable
fun SpaceSegmentChips(
    chips: List<SpaceChip>,
    onSelectMain: () -> Unit,
    onSelectDual: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(PrismRadius.Lg),
        color = LocalPrismExtraColors.current.track,
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyRow(
            contentPadding = PaddingValues(PrismSpacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(PrismSpacing.Xs),
        ) {
            items(chips, key = { it.id }) { chip ->
                val selected = chip.selected
                Surface(
                    shape = RoundedCornerShape(PrismRadius.Md),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = if (chip.isCreate)
                        BorderStroke(PrismSpacing.Hair, MaterialTheme.colorScheme.outlineVariant) else null,
                    shadowElevation = if (selected) 3.dp else 0.dp,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable {
                            when {
                                chip.isCreate -> onCreate()
                                chip.id == "main" -> onSelectMain()
                                else -> onSelectDual(chip.id)
                            }
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = PrismSpacing.Lg, vertical = PrismSpacing.Md),
                    ) {
                        if (chip.isCreate) {
                            Icon(
                                imageVector = PrismIcons.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            // Create chip label is i18n'd here (the chip data carries a Chinese default).
                            text = if (chip.isCreate) stringResource(R.string.lz_space_new_space) else chip.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                chip.isCreate -> MaterialTheme.colorScheme.primary
                                selected -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
