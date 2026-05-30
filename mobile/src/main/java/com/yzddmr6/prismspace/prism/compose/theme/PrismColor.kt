package com.yzddmr6.prismspace.prism.compose.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Fixed light palette ──────────────────────────────────────────────────────
internal val PrismPrimary          = Color(0xFF2F6FED)
internal val PrismOnPrimary        = Color(0xFFFFFFFF)
internal val PrismPrimaryContainer = Color(0xFFDCE8FF)
internal val PrismOnPrimaryContainer= Color(0xFF0A2C66)

internal val PrismBackground       = Color(0xFFF4F6FB)
internal val PrismSurface          = Color(0xFFFFFFFF)
internal val PrismSurfaceVariant   = Color(0xFFEEF2F8)
internal val PrismTrack            = Color(0xFFE5EBF4)
internal val PrismOnSurface        = Color(0xFF161B22)
internal val PrismOnSurfaceVariant = Color(0xFF5A6577)

internal val PrismOutline          = Color(0x17001020) // #0F172A at ~9% alpha ≈ 0x17
// Card border token: #0F172A at 9% alpha — visible on PrismBackground (#F4F6FB), crisp edge
internal val PrismCardBorder       = Color(0xFF0F172A).copy(alpha = 0.09f)
internal val PrismError            = Color(0xFFDC2626)
internal val PrismOnError          = Color(0xFFFFFFFF)
internal val PrismErrorContainer   = Color(0xFFFCE4E4)

// ── Dark palette (M3 dark conventions; mirrors the brand blue, lifted for dark surfaces) ──
internal val PrismPrimaryD          = Color(0xFFACC7FF)
internal val PrismOnPrimaryD        = Color(0xFF06294F)
internal val PrismPrimaryContainerD = Color(0xFF1C3B6E)
internal val PrismOnPrimaryContainerD = Color(0xFFDCE8FF)

internal val PrismBackgroundD       = Color(0xFF0F1115)
internal val PrismSurfaceD          = Color(0xFF171A21)
internal val PrismSurfaceVariantD   = Color(0xFF222834)
internal val PrismTrackD            = Color(0xFF2A2F3A)
internal val PrismOnSurfaceD        = Color(0xFFE6E9EF)
internal val PrismOnSurfaceVariantD = Color(0xFF9AA3B2)

internal val PrismOutlineD          = Color(0x33FFFFFF) // white ~20% — visible hairline on dark surfaces
internal val PrismCardBorderD       = Color(0xFFFFFFFF).copy(alpha = 0.10f)
internal val PrismErrorD            = Color(0xFFFF6B6B)
internal val PrismOnErrorD          = Color(0xFF3A0B0B)
internal val PrismErrorContainerD   = Color(0xFF4A1D1D)

// ── Extra semantic colors exposed via CompositionLocal ───────────────────────
data class PrismExtraColors(
    val ok: Color,
    val okContainer: Color,
    val warn: Color,
    val warnContainer: Color,
    val info: Color,
    val infoContainer: Color,
    // Non-scheme tokens that must still flip with the theme (used directly by components).
    val cardBorder: Color,
    val track: Color,
)

internal val PrismExtraLight = PrismExtraColors(
    ok            = Color(0xFF16A34A),
    okContainer   = Color(0xFFDCF7E5),
    warn          = Color(0xFFB7791F),
    warnContainer = Color(0xFFFBEFD6),
    info          = Color(0xFF2F6FED),
    infoContainer = Color(0xFFE2ECFF),
    cardBorder    = PrismCardBorder,
    track         = PrismTrack,
)

internal val PrismExtraDark = PrismExtraColors(
    ok            = Color(0xFF3ECF8E),
    okContainer   = Color(0xFF143422),
    warn          = Color(0xFFFFB454),
    warnContainer = Color(0xFF3A2E16),
    info          = Color(0xFF5B9DFF),
    infoContainer = Color(0xFF16243A),
    cardBorder    = PrismCardBorderD,
    track         = PrismTrackD,
)

val LocalPrismExtraColors = staticCompositionLocalOf { PrismExtraLight }
