package com.yzddmr6.prismspace.controller

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
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
import android.os.PersistableBundle
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
        if (code == TRANSACTION_SETUP_PROFILE) {
            val adminFlat = data.readString()!!
            val parentUserId = data.readInt()
            val enginePkg = data.readString()!!
            DiagnosticLog.i(TAG, "setupManagedProfile admin=$adminFlat parent=$parentUserId engine=$enginePkg")
            try {
                val result = setupManagedProfile(getSystemContext(), adminFlat, parentUserId, enginePkg)
                reply?.writeInt(result)
            } catch (e: Exception) {
                DiagnosticLog.e(TAG, "setupManagedProfile failed", e)
                reply?.writeInt(-1)
            }
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
     * Create a managed profile and set up PrismSpace as profile owner, all via DevicePolicyManager
     * hidden APIs. Unlike shell commands (pm/dpm/am), these APIs work in the Dhizuku server process
     * because DPM authorizes based on Device Owner identity, not shell UID.
     *
     * Steps: createAndManageUser (creates profile + sets profile owner) → install engine → start user.
     * Returns the new user id, or -1 on failure.
     */
    private fun setupManagedProfile(context: Context, adminFlat: String, parentUserId: Int, enginePkg: String): Int {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager

        // 1. Get Dhizuku's Device Owner ComponentName (the admin that DPM will authorize).
        //    getDeviceOwnerComponentOnAnyUser is @SystemApi — use reflection.
        val deviceOwner = try {
            DevicePolicyManager::class.java
                .getMethod("getDeviceOwnerComponentOnAnyUser")
                .invoke(dpm) as? ComponentName
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "getDeviceOwnerComponentOnAnyUser failed", e)
            null
        }
        if (deviceOwner == null) {
            DiagnosticLog.e(TAG, "setupManagedProfile: no device owner found")
            return -1
        }

        // 2. Create managed profile + set profile owner via DPM.createAndManageUser (hidden API).
        //    This is the Device-Owner-friendly equivalent of `pm create-user --managed` + `dpm set-profile-owner`.
        val prismAdmin = ComponentName.unflattenFromString(adminFlat) ?: run {
            DiagnosticLog.e(TAG, "setupManagedProfile: invalid admin component: $adminFlat")
            return -1
        }
        DiagnosticLog.i(TAG, "createAndManageUser deviceOwner=$deviceOwner profileOwner=$prismAdmin")

        val userHandle: UserHandle? = try {
            val createMethod = DevicePolicyManager::class.java.getMethod(
                "createAndManageUser",
                ComponentName::class.java, String::class.java, ComponentName::class.java,
                PersistableBundle::class.java, Int::class.javaPrimitiveType
            )
            // flags = 2 (SKIP_SETUP_WIZARD) so the new profile doesn't show a setup wizard.
            createMethod.invoke(dpm, deviceOwner, "PrismSpace", prismAdmin, PersistableBundle(), 2) as? UserHandle
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "createAndManageUser failed", e)
            null
        }
        if (userHandle == null) {
            DiagnosticLog.e(TAG, "createAndManageUser returned null")
            return -1
        }

        // UserHandle.getIdentifier() is @hide — use reflection.
        val userId = try {
            UserHandle::class.java.getMethod("getIdentifier").invoke(userHandle) as Int
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "Failed to get user id from UserHandle", e)
            return -1
        }
        DiagnosticLog.i(TAG, "Created managed profile userId=$userId")

        // 3. Install engine into the new profile (engine is the PrismSpace app itself, already
        //    installed in the parent user, so installExistingPackage copies it across).
        try {
            val profileContext = ContextShuttle.createContextAsUser(context, UserHandles.of(userId))
            if (profileContext != null) {
                val pm = profileContext.packageManager
                if (SDK_INT >= Q) {
                    pm.packageInstaller.installExistingPackage(enginePkg, INSTALL_REASON_USER, null)
                } else {
                    PackageManager::class.java
                        .getMethod("installExistingPackageAsUser", String::class.java, Int::class.java)
                        .invoke(pm, enginePkg, userId)
                }
                // Poll briefly — installExistingPackage is fast for an already-on-device package.
                repeat(20) {
                    if (isInstalledForUser(pm, enginePkg)) {
                        DiagnosticLog.i(TAG, "Engine installed userId=$userId poll=$it")
                        return@repeat
                    }
                    Thread.sleep(150)
                }
            }
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "Engine install failed userId=$userId", e)
        }

        // 4. Start the new user so the profile becomes active.
        try {
            val startUserMethod = UserManager::class.java.getMethod("startUser", Int::class.javaPrimitiveType)
            startUserMethod.invoke(um, userId)
            DiagnosticLog.i(TAG, "User started userId=$userId")
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "startUser failed userId=$userId", e)
        }

        return userId
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
        const val TRANSACTION_SETUP_PROFILE = IBinder.FIRST_CALL_TRANSACTION + 3
        const val APP_OP_MODE_ALLOWED = AppOpsManager.MODE_ALLOWED
        const val APP_OP_MODE_IGNORED = AppOpsManager.MODE_IGNORED
        private const val TAG = "Prism.PRW"
    }
}
