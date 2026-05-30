package com.yzddmr6.prismspace

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import com.yzddmr6.prismspace.provisioning.PrismProvisioning
import com.yzddmr6.prismspace.util.ProfileUser

/**
 * Handles events related to managed profile.
 */
class PrismDeviceAdminReceiver : DeviceAdminReceiver() {

	@ProfileUser override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        // DevicePolicyManager.ACTION_PROVISIONING_SUCCESSFUL is used instead of this trigger on Android O+.
		if (SDK_INT < O) PrismProvisioning.onProfileProvisioningComplete(context, intent)
	}
}
