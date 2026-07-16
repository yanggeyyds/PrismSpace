package com.yzddmr6.prismspace.setup.compose

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.yzddmr6.prismspace.help.PrismHelp
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.setup.PrismSetup
import com.yzddmr6.prismspace.setup.SetupViewModel
import com.yzddmr6.prismspace.util.Activities
import kotlinx.coroutines.flow.StateFlow

/**
 * Activity-scoped bridge between Compose UI and the [SetupViewModel] state machine.
 *
 * The controller does not own mutable state. All UI state lives in
 * [SetupStateViewModel], and
 * the [provisionLauncher] is owned by the Activity (registration must run
 * before STARTED, which forbids retention across recreation). On rotation the
 * Activity rebuilds the controller; the VM keeps the [SetupUiState.Error]
 * dialog alive and the [SetupStateViewModel.incompleteSetupAcked] flag sticky.
 */
class SetupController(
    private val activity: ComponentActivity,
    private val stateVm: SetupStateViewModel,
    private val provisionLauncher: ActivityResultLauncher<Intent>,
) {

    val uiState: StateFlow<SetupUiState> get() = stateVm.uiState

    /** Primary CTA tapped (welcome state or retry-from-error). */
    fun onPrimaryCta() {
        val errorVm = SetupViewModel.checkManagedProvisioningPrerequisites(activity, stateVm.incompleteSetupAcked)
        if (errorVm != null) {
            stateVm.setUiState(errorVm.toErrorState())
            return
        }
        // checkManagedProvisioningPrerequisites returned null — either standard provisioning
        // is allowed, or Dhizuku is the device owner AND authorized. When Dhizuku is the device
        // owner, the standard ACTION_PROVISION_MANAGED_PROFILE intent is rejected by the OS
        // (a device owner already exists), so route through the Dhizuku privileged worker.
        // We check only the device-owner package here — authorization was already verified
        // by checkManagedProvisioningPrerequisites, so no redundant reflection is needed.
        val deviceOwner = com.yzddmr6.prismspace.util.DevicePolicies(activity).getDeviceOwner()
        if ("com.rosan.dhizuku" == deviceOwner) {
            PrismSetup.requestProfileOwnerSetupWithDhizuku(Activities.findActivityFrom(activity))
            return
        }
        launchManagedProvisioning()
    }

    /** Extra action button (from error state) tapped. */
    fun onExtraAction(@StringRes extraActionRes: Int) {
        when (extraActionRes) {
            R.string.button_setup_help -> PrismHelp.showSetupHelp(activity)
            R.string.button_have_checked -> {
                stateVm.incompleteSetupAcked = true
                stateVm.setUiState(SetupUiState.Welcome)
            }
            R.string.button_setup_space_with_root -> {
                PrismSetup.requestProfileOwnerSetupWithRoot(Activities.findActivityFrom(activity))
            }
            R.string.button_setup_space_with_dhizuku -> {
                // Dhizuku is the device owner but hasn't authorized PrismSpace yet.
                // Request Dhizuku permission; on grant, proceed with the Dhizuku
                // provisioning path (which bypasses the standard intent that the OS
                // rejects when a device owner already exists).
                requestDhizukuPermissionAndSetup()
            }
            else -> Log.w(TAG, "Unhandled extra action: $extraActionRes")
        }
    }

    /** "Having trouble?" link from welcome state. */
    fun onShowHelp() {
        PrismHelp.showSetupHelp(activity)
    }

    /** Dismiss error and return to welcome. */
    fun onDismissError() {
        stateVm.setUiState(SetupUiState.Welcome)
    }

    /**
     * Requests Dhizuku permission. On grant, kicks off the Dhizuku provisioning path
     * (create managed profile via privileged worker). On denial or failure, stays on
     * the current error state so the user can retry.
     */
    private fun requestDhizukuPermissionAndSetup() {
        val context = activity.applicationContext
        val initialized = try { Dhizuku.init(context) } catch (e: RuntimeException) { false }
        if (!initialized) {
            Log.w(TAG, "Dhizuku not available — cannot request permission")
            return
        }
        try {
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        PrismSetup.requestProfileOwnerSetupWithDhizuku(Activities.findActivityFrom(activity))
                    } else {
                        Log.w(TAG, "Dhizuku permission denied by user")
                    }
                }
            })
        } catch (e: RuntimeException) {
            Log.w(TAG, "Dhizuku requestPermission failed", e)
        }
    }

    private fun launchManagedProvisioning() {
        val intent = SetupViewModel.buildManagedProfileProvisioningIntentPublic(activity)
        try {
            provisionLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Managed provisioning activity not found", e)
            stateVm.setUiState(SetupUiState.Error(
                messageRes = R.string.setup_error_missing_managed_provisioning,
                messageParams = null,
                extraActionRes = R.string.button_setup_help,
            ))
        }
    }

    companion object {
        private const val TAG = "Prism.SetupCtrl"

        /**
         * Activity-scoped launcher callback. Hoisted to a static so the Activity's
         * ActivityResultLauncher (rebuilt on every recreate) does not need to hold a
         * reference to a stale Controller — it dispatches directly into the retained VM.
         */
        @JvmStatic fun handleProvisionResult(activity: Activity, vm: SetupStateViewModel, resultCode: Int) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "Managed provisioning finished — closing setup activity.")
                    activity.finish()
                }
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "Managed provisioning was cancelled — show cancel recovery options.")
                    vm.setUiState(SetupUiState.Error(
                        messageRes = R.string.setup_solution_for_cancelled_provision,
                        messageParams = null,
                        extraActionRes = R.string.button_setup_space_with_root,
                    ))
                }
                else -> Log.w(TAG, "Unexpected provision resultCode=$resultCode")
            }
        }
    }
}

/** Compose-friendly UI state derived from [SetupViewModel]. */
sealed interface SetupUiState {
    /** Initial welcome screen — guided install pitch. */
    data object Welcome : SetupUiState

    /** Error pane shown when prerequisites fail or provisioning is cancelled. */
    data class Error(
        @StringRes val messageRes: Int,
        /** Message format args. List (not Array) so data-class equality is content-based. */
        val messageParams: List<String>?,
        @StringRes val extraActionRes: Int?,
    ) : SetupUiState
}

/** Adapter for setup prerequisite state. Private to keep it out of the public API. */
private fun SetupViewModel.toErrorState(): SetupUiState.Error {
    @Suppress("UNCHECKED_CAST")
    val params = (message_params as? Array<Any?>)?.map { it?.toString() ?: "" }
    return SetupUiState.Error(
        messageRes = message,
        messageParams = params,
        extraActionRes = action_extra.takeIf { it != 0 },
    )
}
