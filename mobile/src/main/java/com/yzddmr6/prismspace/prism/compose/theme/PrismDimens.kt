package com.yzddmr6.prismspace.prism.compose.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens. Single source of truth for spacing + corner radii so the whole app shares one
 * rhythm instead of scattered ad-hoc dp values.
 *
 * Spacing scale is the 4/8/12/16/24/32 step set. Migration is value-preserving — a token equals the
 * literal it replaces — so tokenizing existing layout does not move pixels; only genuine off-scale
 * outliers (e.g. a stray 6/10/20 gap) are snapped to the nearest step.
 */
object PrismSpacing {
    val None = 0.dp
    val Hair = 1.dp   // borders / dividers (legitimately sub-scale)
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
}

/** Corner-radius tokens. Component-specific shapes map to the nearest named radius. */
object PrismRadius {
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Pill = 26.dp   // setup hero CTA pill
}
