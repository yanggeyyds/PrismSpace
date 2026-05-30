package com.yzddmr6.prismspace.prism.compose.space

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import com.yzddmr6.prismspace.mobile.BuildConfig
import com.yzddmr6.prismspace.util.DeviceAdmins
import com.yzddmr6.prismspace.util.DevicePolicies
import com.yzddmr6.prismspace.util.Hack
import com.yzddmr6.prismspace.util.Hacks
import com.yzddmr6.prismspace.util.Modules
import com.yzddmr6.prismspace.util.Users
import com.yzddmr6.prismspace.util.Users.Companion.toId
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Root-gated create/delete of PrismSpace-managed profile spaces. PUBLIC APIs only;
 *  pure parsing/decisions delegated to SpaceProvisioningParsers. Activity-free. */
object SpaceProvisioningEngine {

    private const val DEFAULT_MAX_USERS_SETPROP = 10  // historical fw.max_users AOSP default; used only when the real cap is Unknown

    private fun rootOk(): Boolean = isRootOutput(Shell.SU.run("id"))

    private fun maxUsers(): Int? =
        Hacks.SystemProperties_getInt.invoke("fw.max_users", -1).statically()
            ?.takeIf { it > 0 }
            ?: runCatching {
                val r = android.content.res.Resources.getSystem()
                val id = r.getIdentifier("config_multiuserMaximumUsers", "integer", "android")
                if (id == 0) null else r.getInteger(id)
            }.getOrNull()

    fun probeMaxSpaces(): SpaceCapProbe =
        computeCap(maxUsers(), Users.getProfilesManagedByPrism().size)

    suspend fun createSpace(context: Context): CreateSpaceResult = withContext(Dispatchers.IO) {
        runCatching { Users.refreshUsers(context) }
            .onFailure { DiagnosticLog.w(TAG, "refresh users before root create failed", it) }
        if (!rootOk()) return@withContext CreateSpaceResult.RootUnavailable
        val probe = probeMaxSpaces()
        (probe as? SpaceCapProbe.Known)
            ?.takeIf { it.current >= it.max }
            ?.let { return@withContext CreateSpaceResult.CapReached(it.max) }
        val cap = (probe as? SpaceCapProbe.Known)?.max ?: DEFAULT_MAX_USERS_SETPROP
        val create = parsePmCreateOutput(Shell.SU.run(listOf(
            "setprop fw.max_users $cap",
            "pm create-user --profileOf ${Users.currentId()} --managed PrismSpace 2>&1", "echo END")))
        when (create) {
            PmCreateOutcome.LimitReached -> return@withContext CreateSpaceResult.CapReached(cap)
            PmCreateOutcome.ManagedProfileLimit -> return@withContext CreateSpaceResult.ManagedProfileLimitReached
            is PmCreateOutcome.Failed -> return@withContext CreateSpaceResult.Failed(create.reason)
            is PmCreateOutcome.Created -> Unit
        }
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val pending = Hack.into(um).with(Hacks.UserManagerHack::class.java)
            .getProfiles(Users.currentId())
            .map { it.getUserHandle() }
            .firstOrNull { it != Users.current() &&
                DevicePolicies.getProfileOwnerAsUser(context, it).let { o -> o == null || !o.isPresent } }
            ?: return@withContext CreateSpaceResult.Failed("created user not found for provisioning")
        val pid = pending.toId()
        val src = context.packageManager.getApplicationInfo(Modules.MODULE_ENGINE, 0).sourceDir
        val admin = DeviceAdmins.getComponentName(context).flattenToString()
        val dbg = if (BuildConfig.DEBUG) "-t " else ""
        val install = Shell.SU.run(
            "settings put global verifier_verify_adb_installs 0 ; " +
            "pm install -r --user $pid $dbg$src ; " +
            "settings put global verifier_verify_adb_installs 1 ; " +
            "dpm set-profile-owner --user $pid $admin && am start-user $pid")
        Users.refreshUsers(context)
        val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return@withContext if (la.getActivityList(context.packageName, pending).isNotEmpty()) {
            DiagnosticLog.i(TAG, "root create success user=$pid")
            CreateSpaceResult.Success(pid)
        } else {
            val reason = install?.joinToString("\n")?.ifBlank { null } ?: "provisioning incomplete"
            DiagnosticLog.w(TAG, "root create incomplete user=$pid reason=$reason")
            CreateSpaceResult.Failed(reason)
        }
    }

    suspend fun deleteSpace(context: Context, space: PrismSpace): DeleteSpaceResult = withContext(Dispatchers.IO) {
        runCatching { Users.refreshUsers(context) }
            .onFailure { DiagnosticLog.w(TAG, "refresh users before root delete failed", it) }
        if (!rootOk()) return@withContext DeleteSpaceResult.RootUnavailable
        when (val r = parsePmRemoveOutput(Shell.SU.run("pm remove-user ${space.userId}"))) {
            PmRemoveOutcome.Removed -> {
                Users.refreshUsers(context)
                DiagnosticLog.i(TAG, "root delete success user=${space.userId}")
                DeleteSpaceResult.Success
            }
            is PmRemoveOutcome.Failed -> {
                DiagnosticLog.w(TAG, "root delete failed user=${space.userId} reason=${r.reason}")
                DeleteSpaceResult.Failed(r.reason)
            }
        }
    }

    private const val TAG = "Prism.SpaceProvision"
}
