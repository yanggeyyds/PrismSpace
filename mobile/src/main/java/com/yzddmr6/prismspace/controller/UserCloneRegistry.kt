package com.yzddmr6.prismspace.controller

import android.content.Context
import android.preference.PreferenceManager

/**
 * Tracks packages the user *explicitly* cloned into a dual space, so the "分身" list/count reflect
 * what the user actually cloned — not the system apps that a managed profile carries by provisioning
 * (Google Play / 设置 / 文件 / 通讯录 …).
 *
 * Membership rule used by the UI (see [SpaceViewModel]/[HomeViewModel]): a dual-space app counts as a
 * 分身 if it is **third-party** (`!isSystem` — third-party apps never auto-provision into a profile, so
 * their presence means the user cloned them) **or** it is a system app recorded here (the user cloned a
 * system app such as Chrome). This registry only needs to capture the latter, rarer case; third-party
 * clones are recognized structurally without it, which also means pre-existing clones need no migration.
 *
 * Stored in the main user's default SharedPreferences (the main app reads it to render the dual list).
 */
object UserCloneRegistry {

    private const val KEY = "prism_user_cloned_pkgs"

    private fun prefs(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    /** Record that the user cloned [pkg] (called from every clone path). Idempotent. */
    fun add(context: Context, pkg: String) {
        val p = prefs(context)
        val current = p.getStringSet(KEY, emptySet()) ?: emptySet()
        if (pkg in current) return
        p.edit().putStringSet(KEY, current + pkg).apply()
    }

    /** Forget a clone (e.g. after the user uninstalls it). Safe if absent. */
    fun remove(context: Context, pkg: String) {
        val p = prefs(context)
        val current = p.getStringSet(KEY, emptySet()) ?: emptySet()
        if (pkg !in current) return
        p.edit().putStringSet(KEY, current - pkg).apply()
    }

    fun contains(context: Context, pkg: String): Boolean =
        (prefs(context).getStringSet(KEY, emptySet()) ?: emptySet()).contains(pkg)
}
