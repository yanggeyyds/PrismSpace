package com.yzddmr6.prismspace.util

import android.content.Context
import android.content.res.Configuration
import android.preference.PreferenceManager
import java.util.Locale

/**
 * Dependency-free per-app language (中/英) override. The app has no appcompat, so instead of
 * AppCompatDelegate.setApplicationLocales we persist the user's choice and wrap each Activity's
 * base context with an overridden [Configuration] locale (works on all API levels).
 *
 * Strings resolve from res/values (English, default) or res/values-zh (Chinese) accordingly.
 * Tags: "system" (follow device), "zh" (Simplified Chinese), "en" (English).
 *
 * Each Activity must call [wrap] in attachBaseContext; the language picker calls [setStored] +
 * Activity.recreate() (and ideally restarts the task) to apply.
 */
object PrismLocale {
    const val SYSTEM = "system"
    const val ZH = "zh"
    const val EN = "en"
    private const val KEY = "prism_locale"

    fun getStored(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .getString(KEY, SYSTEM) ?: SYSTEM

    fun setStored(context: Context, tag: String) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit().putString(KEY, tag).apply()
    }

    private fun localeFor(tag: String): Locale? = when (tag) {
        ZH -> Locale.SIMPLIFIED_CHINESE
        EN -> Locale.ENGLISH
        else -> null   // SYSTEM → no override
    }

    /** Wrap [base] with the stored locale; returns [base] unchanged when following the system. */
    @JvmStatic
    fun wrap(base: Context): Context {
        val locale = localeFor(getStored(base)) ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
