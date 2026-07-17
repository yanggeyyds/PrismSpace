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
     * Find the Device Owner's DeviceAdminReceiver ComponentName using public APIs only.
     *
     * getDeviceOwnerComponentOnAnyUser() is @SystemApi requiring MANAGE_PROFILE_AND_DEVICE_OWNERS
     * permission — not available to app-UID processes. Instead, we use the public getActiveAdmins()
     * which returns all active device admins; we filter for the Dhizuku package to ensure we get
     * the correct admin (the device may have multiple admins from different apps).
     */
    private fun findDeviceOwnerAdmin(dpm: DevicePolicyManager): ComponentName? {
        val admins = try { dpm.activeAdmins } catch (e: Exception) {
            DiagnosticLog.e(TAG, "getActiveAdmins failed", e)
            null
        }
        if (admins != null && admins.isNotEmpty()) {
            DiagnosticLog.i(TAG, "getActiveAdmins returned: $admins")
            // Prefer Dhizuku's admin (package = "com.rosan.dhizuku"); fall back to first admin.
            val dhizukuAdmin = admins.firstOrNull { it.packageName == "com.rosan.dhizuku" }
            val admin = dhizukuAdmin ?: admins.first()
            DiagnosticLog.i(TAG, "Selected device owner admin: $admin (dhizuku=${dhizukuAdmin != null})")
            return admin
        }
        // Fallback: try hidden API getDeviceOwnerComponentOnAnyUser (may throw SecurityException).
        return try {
            DevicePolicyManager::class.java
                .getMethod("getDeviceOwnerComponentOnAnyUser")
                .invoke(dpm) as? ComponentName
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "getDeviceOwnerComponentOnAnyUser fallback failed", e)
            null
        }
    }

    /**
     * Create a managed profile and set up PrismSpace as profile owner, all via DevicePolicyManager
     * hidden APIs. Unlike shell commands (pm/dpm/am), these APIs work in the Dhizuku server process
     * because DPM authorizes based on Device Owner identity, not shell UID.
     *
     * CRITICAL: the [context] must be the Dhizuku app's own Application context (via
     * [getPrivilegedContext]), NOT getSystemContext(). DPM's getCallerIdentity() checks both the
     * calling UID AND the calling package name against the Device Owner's registered package.
     *
     * Steps:
     * 1. Find Device Owner admin via getActiveAdmins() (public API)
     * 2. Snapshot existing profiles to detect hidden API stub
     * 3. createAndManageUser — creates managed profile + sets PrismSpace as profile owner.
     *    DPM internally installs the profile owner's package into the new user if not present.
     * 4. Verify the new profile actually appeared in userProfiles
     * 5. Install engine into the new profile
     * 6. Start the new user.
     * Returns the new user id (≥0) on success, or -1 on failure.
     */
    private fun setupManagedProfile(context: Context, adminFlat: String, parentUserId: Int, enginePkg: String): Int {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        DiagnosticLog.i(TAG, "setupManagedProfile ctxPkg=${context.packageName} admin=$adminFlat parent=$parentUserId engine=$enginePkg")

        // 1. Find the Device Owner's admin ComponentName (Dhizuku's DeviceAdminReceiver).
        val deviceOwnerAdmin = findDeviceOwnerAdmin(dpm) ?: run {
            DiagnosticLog.e(TAG, "setupManagedProfile: no device owner admin found")
            return -1
        }
        DiagnosticLog.i(TAG, "Device owner admin: $deviceOwnerAdmin")

        // 2. Parse PrismSpace's admin ComponentName (the intended profile owner).
        val prismAdmin = ComponentName.unflattenFromString(adminFlat) ?: run {
            DiagnosticLog.e(TAG, "setupManagedProfile: invalid admin component: $adminFlat")
            return -1
        }

        // 3. Snapshot existing profiles before createAndManageUser, so we can verify the
        //    new profile was actually created (hidden API may be silently stubbed on newer OS).
        val usersBefore = um.userProfiles.map { it.hashCode() }.toSet()

        // 4. Create managed profile + set profile owner via DPM.createAndManageUser (hidden API).
        //    DPM will:
        //    a) create the managed-profile user
        //    b) install the profile owner's package (PrismSpace/engine) into the new user if needed
        //    c) set the profile owner to prismAdmin
        //    flags = 2 (SKIP_SETUP_WIZARD) so the new profile doesn't show a setup wizard.
        val userHandle: UserHandle? = try {
            val createMethod = DevicePolicyManager::class.java.getMethod(
                "createAndManageUser",
                ComponentName::class.java, String::class.java, ComponentName::class.java,
                PersistableBundle::class.java, Int::class.javaPrimitiveType
            )
            createMethod.invoke(dpm, deviceOwnerAdmin, "PrismSpace", prismAdmin, PersistableBundle(), 2) as? UserHandle
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "createAndManageUser failed (admin=$deviceOwnerAdmin profileOwner=$prismAdmin)", e)
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
        DiagnosticLog.i(TAG, "createAndManageUser returned userId=$userId")

        // Verify the new profile actually exists in the user list. On Android 14+ the hidden API
        // may be silently stubbed (returns a fake UserHandle without creating a profile), in which
        // case userProfiles won't contain the returned userId.
        val usersAfter = um.userProfiles.map { it.hashCode() }.toSet()
        val newUsers = usersAfter - usersBefore
        if (userId !in newUsers) {
            DiagnosticLog.e(TAG, "createAndManageUser stub detected: returned userId=$userId but no new user appeared (before=$usersBefore after=$usersAfter new=$newUsers)", null)
            return -1
        }
        DiagnosticLog.i(TAG, "Verified new managed profile userId=$userId")

        // 5. Ensure engine is installed in the new profile (createAndManageUser may already do this,
        //    but verify + install as fallback — needed on some Android versions).
        try {
            val profileContext = ContextShuttle.createContextAsUser(context, UserHandles.of(userId))
            if (profileContext != null) {
                val pm = profileContext.packageManager
                if (!isInstalledForUser(pm, enginePkg)) {
                    DiagnosticLog.i(TAG, "Engine not installed yet, installing userId=$userId")
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
                } else {
                    DiagnosticLog.i(TAG, "Engine already installed userId=$userId (createAndManageUser handled it)")
                }
            }
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "Engine install failed userId=$userId", e)
        }

        // 6. Start the new user so the profile becomes active.
        //    隐藏 API startUser(int) 在 Android 14+ 可能被拦截；若失败，profile 虽创建但无法激活。
        try {
            val startUserMethod = UserManager::class.java.getMethod("startUser", Int::class.javaPrimitiveType)
            startUserMethod.invoke(um, userId)
            DiagnosticLog.i(TAG, "User started userId=$userId")
        } catch (e: Exception) {
            DiagnosticLog.e(TAG, "startUser failed — user $userId was created but cannot be activated", e)
            return -1
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
