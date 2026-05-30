package com.yzddmr6.prismspace.prism.service

import android.content.Context
import android.preference.PreferenceManager
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import org.json.JSONArray
import org.json.JSONObject

/** One persisted file-transfer record (survives app restart, unlike the old in-memory list). */
data class TransferRecord(
    val name: String,
    /** Full package name for APK/clone transfers (shown as "label-package"); null for plain files. */
    val packageName: String?,
    val location: String,   // human-readable destination, e.g. "下载/PrismSpace"
    val isImage: Boolean,
    val timeMillis: Long,
)

/**
 * Unify the transfer-record title across the main space (Files tab) and the dual space (entry
 * screen) to "应用名-包名" (e.g. Chrome-com.android.chrome) for app/APK clones; plain files (images,
 * docs) have no package, so they fall back to the file name.
 */
fun TransferRecord.displayTitle(): String =
    if (! packageName.isNullOrBlank()) "$name-$packageName" else name

data class TransferRecordUiActions(
    val canOpenWithFileManager: Boolean,
    val canInstall: Boolean,
)

object TransferRecordActions {
    fun forRecord(record: TransferRecord, hasInstallableApkSet: Boolean): TransferRecordUiActions =
        TransferRecordUiActions(
            canOpenWithFileManager = true,
            canInstall = !record.packageName.isNullOrBlank() && hasInstallableApkSet,
        )
}

/**
 * Persists the file-transfer history per user (SharedPreferences-backed, mirrors [ExperimentalFlags]
 * / SharedPrefsModeStore). The receiver records here when a share is imported into this space; the
 * Files page reads it. Per-user by construction: each profile's PrismSpace has its own prefs, so the
 * main space shows main imports and the dual space shows dual imports.
 */
object TransferHistoryStore {

    private const val KEY = "prism_transfer_history"
    private const val MAX = 50

    fun record(context: Context, name: String, location: String, isImage: Boolean, packageName: String? = null) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val arr = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        val entry = JSONObject().apply {
            put("name", name)
            if (! packageName.isNullOrBlank()) put("pkg", packageName)
            put("location", location)
            put("isImage", isImage)
            put("time", System.currentTimeMillis())
        }
        // newest first, capped
        val out = JSONArray().apply { put(entry) }
        for (i in 0 until minOf(arr.length(), MAX - 1)) out.put(arr.get(i))
        // commit() (synchronous), NOT apply(): the dual-space record is written inside a Shuttle
        // closure running in the profile process, which can be torn down immediately after the
        // closure returns — an async apply() may never flush, so the record silently vanishes.
        prefs.edit().putString(KEY, out.toString()).commit()
        DiagnosticLog.i(TAG, "transfer recorded name=$name pkg=${packageName ?: ""} location=$location isImage=$isImage")
    }

    fun load(context: Context): List<TransferRecord> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val arr = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    TransferRecord(
                        name = o.optString("name", "file"),
                        packageName = o.optString("pkg", "").ifBlank { null },
                        location = o.optString("location", ""),
                        isImage = o.optBoolean("isImage", false),
                        timeMillis = o.optLong("time", 0L),
                    )
                )
            }
        }
    }

    fun clear(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit().remove(KEY).commit()
        DiagnosticLog.i(TAG, "transfer history cleared")
    }
}

private const val TAG = "Prism.TransferHistory"
