package com.yzddmr6.prismspace.controller

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.INSTALL_REASON_USER
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.IBinder
import android.os.Parcel
import android.os.UserHandle
import android.os.UserManager
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
        val profileContext = ContextShuttle.createContextAsUser(context, UserHandles.of(userId))
        val pm = profileContext?.packageManager
        if (pm != null && isInstalledForUser(pm, pkg)) {
            DiagnosticLog.i(TAG, "cloneAppViaPrivileged already installed pkg=$pkg userId=$userId")
            return true
        }
        if (pm != null && SDK_INT >= Q) {
            pm.packageInstaller.installExistingPackage(pkg, INSTALL_REASON_USER, null)
        } else {
            // Use hidden installExistingPackageAsUser which takes userId directly and
            // works without needing a per-user context (needed for Dhizuku app-UID process).
            val installed = try {
                val method = PackageManager::class.java.getMethod(
                    "installExistingPackageAsUser", String::class.java, Int::class.java)
                method.invoke(pm ?: context.packageManager, pkg, userId) as Int
            } catch (e: PackageManager.NameNotFoundException) { return false }
            if (installed == INSTALL_SUCCEEDED) {
                DiagnosticLog.i(TAG, "cloneAppViaPrivileged installed pkg=$pkg userId=$userId")
                return true
            }
        }
        // installExistingPackage is async. Poll the real installed state (<=3s).
        val checkInstalled: (String) -> Boolean = if (pm != null) {
            { p -> isInstalledForUser(pm, p) }
        } else {
            { p -> isInstalledForUserId(context.packageManager, p, userId) }
        }
        repeat(20) {
            if (checkInstalled(pkg)) {
                DiagnosticLog.i(TAG, "cloneAppViaPrivileged installed pkg=$pkg userId=$userId poll=$it")
                return true
            }
            try { Thread.sleep(150) } catch (e: InterruptedException) { Thread.currentThread().interrupt(); return checkInstalled(pkg) }
        }
        val installed = checkInstalled(pkg)
        DiagnosticLog.i(TAG, "cloneAppViaPrivileged finished pkg=$pkg userId=$userId installed=$installed")
        return installed
    }

    private fun isInstalledForUser(pm: PackageManager, pkg: String): Boolean =
        try { pm.getApplicationInfo(pkg, 0); true } catch (e: PackageManager.NameNotFoundException) { false }

    /** Check if a package is installed for a specific userId using hidden API. */
    private fun isInstalledForUserId(pm: PackageManager, pkg: String, userId: Int): Boolean = try {
        val method = PackageManager::class.java.getMethod(
            "getApplicationInfoAsUser", String::class.java, Int::class.java, Int::class.java)
        method.invoke(pm, pkg, 0, userId)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Quick probe: can this context create a per-user package context? */
    private fun hasCrossUserAccess(context: Context): Boolean = try {
        val method = Context::class.java.getMethod(
            "createPackageContextAsUser", String::class.java, Int::class.java, UserHandle::class.java)
        method.invoke(context, context.packageName, 0, android.os.Process.myUserHandle())
        true
    } catch (_: Exception) {
        false
    }

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
                val (result, _) = execShell(command ?: "")
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
                // Try getSystemContext() first (works for Shizuku/shell UID).
                // For Dhizuku (app UID, Device Owner), getSystemContext() may not have
                // INTERACT_ACROSS_USERS for createPackageContextAsUser, so fallback to
                // getPrivilegedContext() which is Dhizuku's own Application context.
                var ctx = runCatching { getSystemContext() }.getOrNull()
                if (ctx == null || !hasCrossUserAccess(ctx)) {
                    ctx = getPrivilegedContext()
                }
                val result = cloneAppViaPrivileged(ctx, pkg, userId)
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
        if (code == TRANSACTION_SETUP_PROFILE) {
            val adminFlat = data.readString()!!
            val parentUserId = data.readInt()
            val enginePkg = data.readString()!!
            DiagnosticLog.i(TAG, "setupManagedProfile admin=$adminFlat parent=$parentUserId engine=$enginePkg")
            try {
                // CRITICAL: Binder.getCallingUid() in onTransact returns the CALLER's UID (PrismSpace),
                // NOT this worker process's UID (Dhizuku). DPM's getCallerIdentity() uses
                // Binder.getCallingUid() to check if the caller is Device Owner — PrismSpace is NOT
                // Device Owner, so createAndManageUser would be rejected.
                // Fix: clear calling identity so DPM sees THIS process's UID (Dhizuku = Device Owner).
                val token = Binder.clearCallingIdentity()
                try {
                    val result = setupManagedProfile(getPrivilegedContext(), adminFlat, parentUserId, enginePkg)
                    reply?.writeInt(result)
                } finally {
                    Binder.restoreCallingIdentity(token)
                }
            } catch (e: Exception) {
                DiagnosticLog.e(TAG, "setupManagedProfile failed", e)
                reply?.writeInt(-1)
            }
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }

    /**
     * Create a managed profile and set up PrismSpace as profile owner via shell commands.
     *
     * Unlike the root path (shell UID), this runs in the Dhizuku server process at application UID
     * (Device Owner). The `pm`/`dpm`/`am` tools communicate with system services via Binder, where
     * the caller's UID is Dhizuku's app UID. Some commands (notably `pm create-user`) require
     * `CREATE_USERS` permission which Dhizuku doesn't have — but on many Android 14+ ROMs,
     * the Device Owner status is sufficient for the underlying service to allow the operation.
     *
     * Steps:
     * 1. `pm create-user --profileOf <parent> --managed PrismSpace`
     * 2. Parse userId from output
     * 3. [engine install handled by createAndManageUser or separately]
     * 4. `dpm set-profile-owner --user <id> <adminFlat>`
     * 5. `am start-user <id>`
     * Returns the new user id (≥0) on success, or -1 on failure.
     */
    private fun setupManagedProfile(context: Context, adminFlat: String, parentUserId: Int, enginePkg: String): Int {
        DiagnosticLog.i(TAG, "setupManagedProfile admin=$adminFlat parent=$parentUserId engine=$enginePkg")

        // 1. Create managed profile via shell.
        val createCmd = "pm create-user --profileOf $parentUserId --managed PrismSpace 2>&1"
        val (createOutput, createExit) = execShell(createCmd)
        DiagnosticLog.i(TAG, "Shell: $createCmd -> exit=$createExit output=$createOutput")
        if (createExit != 0) {
            DiagnosticLog.e(TAG, "pm create-user failed exit=$createExit: $createOutput", null)
            return -1
        }

        val userId = parseUserIdFromShellOutput(createOutput) ?: run {
            DiagnosticLog.e(TAG, "Failed to parse userId from: $createOutput", null)
            return -1
        }
        DiagnosticLog.i(TAG, "Created managed profile userId=$userId")

        // 2. Set profile owner via DPM shell command.
        val dpmCmd = "dpm set-profile-owner --user $userId $adminFlat 2>&1"
        val (dpmOutput, dpmExit) = execShell(dpmCmd)
        DiagnosticLog.i(TAG, "Shell: $dpmCmd -> exit=$dpmExit output=$dpmOutput")

        // 3. Start the new user.
        val startCmd = "am start-user $userId 2>&1"
        val (startOutput, startExit) = execShell(startCmd)
        DiagnosticLog.i(TAG, "Shell: $startCmd -> exit=$startExit output=$startOutput")

        return userId
    }

    /** Extract userId from `pm create-user` or `cmd` output (various formats). */
    private fun parseUserIdFromShellOutput(output: String): Int? {
        val cleaned = output.trim()
        val idMatch = Regex("""(\d+)""").find(cleaned)
        return idMatch?.groupValues?.get(1)?.toIntOrNull()
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
     * Returns the privileged app's own Application context.
     *
     * When running inside the Dhizuku server process (via bindUserService), this returns Dhizuku's
     * Application context — whose opPackageName is "com.rosan.dhizuku" (the Device Owner's package).
     * This is CRITICAL for DPM calls: DevicePolicyManagerService.getCallerIdentity() verifies that
     * the calling package matches the Device Owner's package. getSystemContext() returns a context
     * with opPackageName="android" which fails this check.
     *
     * When running inside the Shizuku server process, this returns Shizuku's Application context
     * (shell UID) — fine for cloneAppViaPrivileged which only uses PackageManager, not DPM.
     */
    private fun getPrivilegedContext(): Context {
        try {
            val classActivityThread = Class.forName("android.app.ActivityThread")
            val at = classActivityThread.getMethod("currentActivityThread").invoke(null)
            val app = classActivityThread.getMethod("currentApplication").invoke(at) as? Context
            if (app != null) {
                DiagnosticLog.i(TAG, "getPrivilegedContext: package=${app.packageName}")
                return app
            }
        } catch (e: ReflectiveOperationException) {
            DiagnosticLog.e(TAG, "getPrivilegedContext: currentApplication failed, falling back to system context", e)
        }
        return getSystemContext()
    }

    /**
     * Execute a shell command in the Dhizuku server process (Device Owner privileges).
     * Used for managed-profile setup: `pm create-user --profileOf ... --managed`,
     * `pm install --user ...`, `dpm set-profile-owner`, `am start-user`.
     */
    private fun execShell(command: String): Pair<String, Int> {
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
        return stdout to exitCode
    }

    init { DiagnosticLog.i(TAG, "Running in privileged service...") }

    companion object {
        private const val INSTALL_SUCCEEDED = 1     // PackageManager.INSTALL_SUCCEEDED
        private const val TRANSACTION_DESTROY = 16777115
        const val TRANSACTION_CLONE_APP = IBinder.FIRST_CALL_TRANSACTION
        const val TRANSACTION_SET_APP_OP_MODE = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_EXEC_SHELL = IBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_SETUP_PROFILE = IBinder.FIRST_CALL_TRANSACTION + 3
        const val APP_OP_MODE_ALLOWED = AppOpsManager.MODE_ALLOWED
        const val APP_OP_MODE_IGNORED = AppOpsManager.MODE_IGNORED
        private const val TAG = "Prism.PRW"
    }
}
