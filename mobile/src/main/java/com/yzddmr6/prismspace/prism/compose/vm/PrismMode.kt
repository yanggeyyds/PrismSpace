package com.yzddmr6.prismspace.prism.compose.vm

import androidx.annotation.StringRes
import com.yzddmr6.prismspace.mobile.R

/**
 * Selected capability mode — single source of truth for which mode the USER
 * has chosen. Detection may still gate gating, but the UI label/checkmark
 * always follows this enum, not a live re-detect.
 */
enum class PrismMode { Normal, Shizuku, Root }

/** Single localizable label resource for a [PrismMode], used by both Settings and Home. */
@StringRes
fun prismModeLabelRes(mode: PrismMode): Int = when (mode) {
    PrismMode.Root -> R.string.lz_mode_root
    PrismMode.Shizuku -> R.string.lz_mode_shizuku
    PrismMode.Normal -> R.string.lz_mode_normal
}
