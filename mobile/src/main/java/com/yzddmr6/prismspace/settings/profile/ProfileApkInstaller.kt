package com.yzddmr6.prismspace.settings.profile

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.PendingIntent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import android.widget.Toast
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.service.FileTransferPolicy
import com.yzddmr6.prismspace.util.PrismLocale

/**
 * Foreground PackageInstaller session for a cloned app's copied APK set (base + splits) that the
 * 普通模式 clone left in the dual space's Download/PrismSpace/.
 *
 * Runs inside the work profile FROM A FOREGROUND screen (the 棱镜-双开空间 entry), which is what makes
 * no-privilege install possible on Android 15+:
 *  - foreground → dodges the BAL block that kills launching the installer from the main space;
 *  - the system "confirm install" UI is routed by PackageInstaller to the real system installer, NOT
 *    via an ACTION_VIEW resolve — so a hijacked APK default handler (e.g. Termux) can't intercept it;
 *  - all splits go into one session → split apps (X/Twitter) actually install.
 */
object ProfileApkInstaller {

    private const val TAG = "Prism.PAI"
    private const val ACTION_RESULT = "com.yzddmr6.prismspace.action.PROFILE_INSTALL_RESULT"
    private const val EXTRA_BASE = "base"

    /** True when the copied base/split APK files for this transfer record still exist in Download/PrismSpace. */
    fun hasCopiedApkSet(context: Context, pkg: String, label: String): Boolean =
        queryApkSet(context.applicationContext, safeBase(label, pkg)).isNotEmpty()

    /**
     * Install the copied APK set for [pkg] cloned under [label]. The clone wrote files named
     * "<safeBase>.apk" + "<safeBase>.splitN.apk" where safeBase = safeDisplayName("label-pkg").
     * Must be called from a foreground context (the entry screen) so the confirm dialog can launch.
     */
    fun install(context: Context, pkg: String, label: String) {
        val appCtx = context.applicationContext
        val loc = PrismLocale.wrap(context)
        DiagnosticLog.i(TAG, "profile apk install requested pkg=$pkg label=$label")
        // A no-privilege session install needs PrismSpace's own "install unknown apps" op.
        // Some ROMs block immediately when it is missing, so guide the user to grant it first.
        // Cleared DISALLOW_INSTALL_UNKNOWN_SOURCES is necessary but not sufficient.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appCtx.packageManager.canRequestPackageInstalls()) {
            DiagnosticLog.i(TAG, "profile apk install needs unknown-source permission pkg=$pkg")
            runCatching {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${appCtx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Toast.makeText(context, loc.getString(R.string.lz_pf_install_need_perm), Toast.LENGTH_LONG).show()
            return
        }
        val safeBase = safeBase(label, pkg)
        val uris = queryApkSet(appCtx, safeBase)
        if (uris.isEmpty()) {
            DiagnosticLog.w(TAG, "profile apk install has no copied apk set pkg=$pkg safeBase=$safeBase")
            Toast.makeText(context, loc.getString(R.string.lz_pf_install_no_apk), Toast.LENGTH_LONG).show(); return
        }
        registerResultReceiver(appCtx)
        DiagnosticLog.i(TAG, "profile apk install session start pkg=$pkg files=${uris.size}")
        Toast.makeText(context, loc.getString(R.string.lz_pf_install_started), Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val installer = appCtx.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                runCatching { params.setAppPackageName(pkg) }
                val sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    uris.forEachIndexed { i, uri ->
                        appCtx.contentResolver.openInputStream(uri)?.use { input ->
                            session.openWrite("split$i.apk", 0, -1).use { out ->
                                input.copyTo(out); session.fsync(out)
                            }
                        } ?: throw IllegalStateException("cannot read $uri")
                    }
                    val callback = Intent(ACTION_RESULT).setPackage(appCtx.packageName).putExtra(EXTRA_BASE, safeBase)
                    val pi = PendingIntent.getBroadcast(
                        appCtx, sessionId, callback,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                    session.commit(pi.intentSender)
                    DiagnosticLog.i(TAG, "profile apk install session committed pkg=$pkg sessionId=$sessionId")
                }
            } catch (e: Exception) {
                DiagnosticLog.e(TAG, "session install failed for $pkg", e)
                showToast(appCtx, loc.getString(R.string.lz_pf_install_failed, e.message ?: e.javaClass.simpleName))
            }
        }.start()
    }

    /** base + splits in Download/PrismSpace named exactly "<base>.apk" / "<base>.splitN.apk" (no MediaStore "(1)" dupes). */
    private fun queryApkSet(context: Context, safeBase: String): List<Uri> {
        val out = ArrayList<Uri>()
        val coll = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val proj = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
        val sel = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%Download/PrismSpace/%", "$safeBase%.apk")
        val exact = Regex("^${Regex.escape(safeBase)}(\\.split\\d+)?\\.apk$")
        runCatching {
            context.contentResolver.query(coll, proj, sel, args, "${MediaStore.MediaColumns.DISPLAY_NAME} ASC")?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (c.moveToNext()) {
                    if (exact.matches(c.getString(nameCol))) out.add(ContentUris.withAppendedId(coll, c.getLong(idCol)))
                }
            }
        }.onFailure { DiagnosticLog.e(TAG, "queryApkSet failed", it) }
        return out
    }

    private fun safeBase(label: String, pkg: String): String =
        FileTransferPolicy.safeDisplayName("$label-$pkg")

    @Volatile private var receiverRegistered = false
    private fun registerResultReceiver(appCtx: Context) {
        if (receiverRegistered) return
        receiverRegistered = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val loc = PrismLocale.wrap(c)
                when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        DiagnosticLog.i(TAG, "profile apk install pending user confirm")
                        // Foreground confirm — the user just tapped 安装, so the app is foreground and BAL allows it.
                        @Suppress("DEPRECATION") val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        if (confirm != null) {
                            confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { c.startActivity(confirm) }
                                .onFailure { DiagnosticLog.e(TAG, "cannot launch install confirm", it) }
                        }
                    }
                    PackageInstaller.STATUS_SUCCESS -> {
                        DiagnosticLog.i(TAG, "profile apk install success")
                        Toast.makeText(c, loc.getString(R.string.lz_pf_install_success), Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status=$status"
                        DiagnosticLog.w(TAG, "profile apk install failed status=$status message=$msg")
                        Toast.makeText(c, loc.getString(R.string.lz_pf_install_failed, msg), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            appCtx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") appCtx.registerReceiver(receiver, filter)
    }

    private fun showToast(appCtx: Context, msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show()
        }
    }
}
