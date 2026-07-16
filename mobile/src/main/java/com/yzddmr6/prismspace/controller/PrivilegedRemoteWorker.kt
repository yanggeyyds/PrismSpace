package com.yzddmr6.prismspace.controller

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.INSTALL_REASON_USER
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.IBinder
import android.os.Parcel
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import com.yzddmr6.prismspace.util.UserHandles
import com.yzddmr6.prismspace.appops.AppOpsCompat
import com.yzddmr6.prismspace.shuttle.ContextShuttle

class PrivilegedRemoteWorker: Binder() {

    /**
     * Runs with privileged permission via Shizuku or Dhizuku (both bind this worker into a privileged
     * server process). installExistingPackage reports no synchronous result, so verify the real
     * installed state for the target user instead of trusting dispatch.
     */
    private fun cloneAppViaPrivileged(context: Context, pkg: String, userId: Int): Boolean {
        DiagnosticLog.i(TAG, "cloneAppViaPrivileged start pkg=$pkg userId=$userId")
        val profileContext = ContextShuttle.createContextAsUser(context, UserHandles.of(userId)) ?: return false
        val pm = profileContext.packageManager
        if (isInstalledForUser(pm, pkg)) {
            DiagnosticLog.i(TAG, "cloneAppViaPrivileged already installed pkg=$pkg userId=$userId")
            return true
        }
        if (SDK_INT >= Q) {
            pm.packageInstaller.installExistingPackage(pkg, INSTALL_REASON_USER, null)
        } else try {   // int installExistingPackageAsUser(String packageName, int userId)
            PackageManager::class.java.getMethod("installExistingPackageAsUser", String::class.java, Int::class.java)
                .invoke(pm, pkg, userId)
        } catch (e: PackageManager.NameNotFoundException) { return false }
        // installExistingPackage is async; an already-on-device package installs fast. Poll the real
        // installed state (<=3s) and report ground truth instead of assuming success.
        repeat(20) {
            if (isInstalledForUser(pm, pkg)) {
                DiagnosticLog.i(TAG, "cloneAppViaPrivileged installed pkg=$pkg userId=$userId poll=$it")
                return true
            }
            try { Thread.sleep(150) } catch (e: InterruptedException) { Thread.currentThread().interrupt(); return isInstalledForUser(pm, pkg) }
        }
        val installed = isInstalledForUser(pm, pkg)
        DiagnosticLog.i(TAG, "cloneAppViaPrivileged finished pkg=$pkg userId=$userId installed=$installed")
        return installed
    }

    private fun isInstalledForUser(pm: PackageManager, pkg: String): Boolean =
        try { pm.getApplicationInfo(pkg, 0); true } catch (e: PackageManager.NameNotFoundException) { false }

    private fun setAppOpModeViaPrivileged(context: Context, pkg: String, userId: Int, op: String, mode: Int): Boolean {
        val profileContext = ContextShuttle.createContextAsUser(context, UserHandles.of(userId)) ?: return false
        val uid = profileContext.packageManager.getPackageUid(pkg, MATCH_DISABLED_COMPONENTS)
        val appOps = AppOpsCompat(profileContext)
        val opCode = appOps.strOpToOp(op)
        appOps.setMode(opCode, uid, pkg, mode)
        return appOps.checkOpNoThrow(opCode, uid, pkg) == mode
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == TRANSACTION_DESTROY) {
            DiagnosticLog.i(TAG, "destroy requested")
            reply?.writeNoException()
            scheduleProcessExit()
            return true
        }
        if (code == TRANSACTION_EXEC_SHELL) {
            val command = data.readString()
            DiagnosticLog.i(TAG, "exec shell: $command")
            try {
                val result = execShell(command ?: "")
                reply?.writeString(result)
            } catch (e: Exception) {
                DiagnosticLog.e(TAG, "exec shell failed", e)
                reply?.writeString(null)
            }
            return true
        }
        if (code == TRANSACTION_CLONE_APP) {
            val pkg = data.readString()!!
            val userId = data.readInt()
            DiagnosticLog.i(TAG, "onTransact clone pkg=$pkg userId=$userId")
            try {
                val result = cloneAppViaPrivileged(getSystemContext(), pkg, userId)
                reply?.writeInt(if (result) 1 else 0) }
            catch (e: Exception) {
                DiagnosticLog.e(TAG, "Error cloning $pkg via privileged service", e)
                reply?.writeInt(-1) }
            return true
        }
        if (code == TRANSACTION_SET_APP_OP_MODE) {
            val pkg = data.readString()!!
            val userId = data.readInt()
            val op = data.readString()!!
            val mode = data.readInt()
            try {
                val result = setAppOpModeViaPrivileged(getSystemContext(), pkg, userId, op, mode)
                reply?.writeInt(if (result) 1 else 0) }
            catch (e: Exception) {
                DiagnosticLog.e(TAG, "Error setting app-op $op to $mode for $pkg via privileged service", e)
                reply?.writeInt(-1) }
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }

    private fun scheduleProcessExit() {
        Thread {
            try { Thread.sleep(100) }
            catch (e: InterruptedException) { Thread.currentThread().interrupt() }
            DiagnosticLog.i(TAG, "Exiting privileged user service process")
            Runtime.getRuntime().exit(0)
        }.apply {
            name = "PrismPrivilegedWorkerExit"
            isDaemon = false
            start()
        }
    }

    private fun getSystemContext(): Context {
        try {
            val classActivityThread = Class.forName("android.app.ActivityThread")
            val at = classActivityThread.getMethod("currentActivityThread").invoke(null)
            val context = classActivityThread.getMethod("getSystemContext").invoke(at) as? Context
            if (context != null) return context }
        catch (e: ReflectiveOperationException) {
            DiagnosticLog.e(TAG, "Error retrieving system context", e) }
        throw UnsupportedOperationException()
    }

    /**
     * Execute a shell command in the Dhizuku server process (Device Owner privileges).
     * Used for managed-profile setup: `pm create-user --profileOf ... --managed`,
     * `pm install --user ...`, `dpm set-profile-owner`, `am start-user`.
     */
    private fun execShell(command: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        // Read stdout and stderr concurrently to avoid deadlock when the OS pipe
        // buffer fills on one stream while we're blocked reading the other.
        val stderrThread = Thread {
            try { process.errorStream.bufferedReader().readText() }
            catch (e: Exception) { DiagnosticLog.e(TAG, "execShell stderr read failed", e) }
        }.also { it.isDaemon = true; it.start() }
        val stdout = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        stderrThread.join(2000)
        DiagnosticLog.i(TAG, "execShell done exit=$exitCode stdout=${stdout.take(200)}")
        return stdout
    }

    init { DiagnosticLog.i(TAG, "Running in privileged service...") }

    companion object {
        private const val INSTALL_SUCCEEDED = 1     // PackageManager.INSTALL_SUCCEEDED
        private const val TRANSACTION_DESTROY = 16777115
        const val TRANSACTION_CLONE_APP = IBinder.FIRST_CALL_TRANSACTION
        const val TRANSACTION_SET_APP_OP_MODE = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_EXEC_SHELL = IBinder.FIRST_CALL_TRANSACTION + 2
        const val APP_OP_MODE_ALLOWED = AppOpsManager.MODE_ALLOWED
        const val APP_OP_MODE_IGNORED = AppOpsManager.MODE_IGNORED
        private const val TAG = "Prism.PRW"
    }
}
