package com.yzddmr6.prismspace.prism.compose.settings

import android.content.Context
import android.preference.PreferenceManager

/** Persistent experimental-feature flags, stored in the default (credential-protected) SharedPreferences. */
object ExperimentalFlags {
    private const val KEY_MULTI_PROFILE = "experimental_multi_profile"

    fun isMultiProfileEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_MULTI_PROFILE, false)

    fun setMultiProfileEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putBoolean(KEY_MULTI_PROFILE, enabled).apply()
    }
}
