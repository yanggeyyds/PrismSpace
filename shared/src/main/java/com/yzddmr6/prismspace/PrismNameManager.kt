package com.yzddmr6.prismspace

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import com.yzddmr6.prismspace.shared.R
import com.yzddmr6.prismspace.shuttle.Shuttle
import com.yzddmr6.prismspace.util.DevicePolicies
import com.yzddmr6.prismspace.util.OwnerUser
import com.yzddmr6.prismspace.util.ProfileUser
import com.yzddmr6.prismspace.util.Users
import com.yzddmr6.prismspace.util.Users.Companion.ACTION_USER_INFO_CHANGED
import com.yzddmr6.prismspace.util.Users.Companion.EXTRA_USER_HANDLE
import com.yzddmr6.prismspace.util.Users.Companion.isParentProfile
import com.yzddmr6.prismspace.util.Users.Companion.toId
import com.yzddmr6.prismspace.util.sendProtectedBroadcastInternally

object PrismNameManager {

	@OwnerUser @JvmStatic fun getAllNames(context: Context): Map<UserHandle, String> {
		val profiles = Users.getProfilesManagedByPrism()
		return when (profiles.size) {
			0 -> emptyMap()
			1 -> mapOf(Pair(profiles[0], context.getString(R.string.default_space_name)))
			else -> getStore(context).let { store -> profiles.associateWith { profile ->
					store.getString(buildPrismNameKey(context, profile), null) ?: getDefaultSpecificName(context, profile) }}}
	}

	@ProfileUser fun getName(context: Context) =
		getStore(context).getString(buildPrismNameKey(context), null) ?: getDefaultName(context)

	private fun getDefaultName(context: Context, profile: UserHandle = Users.current()): String {
		if (profile.isParentProfile()) return context.getString(R.string.mainland_name)
		val prismCount = Users.run { if (isParentProfile()) getProfilesManagedByPrism().size else getProfileCount() - 1 }
		return if (prismCount > 1) getDefaultSpecificName(context, profile) else context.getString(R.string.default_space_name)
	}

	private fun getDefaultSpecificName(context: Context, profile: UserHandle = Users.current()) =
		when (val profileId = profile.toId()) {
			0    -> context.getString(R.string.mainland_name)
			10   -> context.getString(R.string.default_space0_name)
			11   -> context.getString(R.string.default_space1_name)
			12   -> context.getString(R.string.default_space2_name)
			13   -> context.getString(R.string.default_space3_name)
			else -> context.getString(R.string.default_spaceN_name, profileId) }

	@OwnerUser @ProfileUser private fun saveProfileName(context: Context, profile: UserHandle?, name: String)
			= getStore(context).edit().putString(buildPrismNameKey(context, profile), name).apply()

	@Suppress("DEPRECATION") private fun getStore(context: Context) =
		android.preference.PreferenceManager.getDefaultSharedPreferences(context.createDeviceProtectedStorageContext())

	private fun buildPrismNameKey(context: Context, user: UserHandle? = null): String {
		val key = context.getString(R.string.key_space_name)
		return if (user != null) "$key.${user.toId()}" else key
	}

	@ProfileUser fun setName(context: Context, name: String) {  // Extra spaces for better readability in system UI (e.g. app Uninstall confirmation dialog)
		DevicePolicies(context).invoke(DevicePolicyManager::setProfileName, " $name ")
		saveProfileName(context, null, name)
		sendProtectedBroadcastInternally(context, Intent(ACTION_USER_INFO_CHANGED).putExtra(EXTRA_USER_HANDLE, Users.currentId()))
		syncNameToParentProfile(context, name)
	}

	@ProfileUser fun syncNameToParentProfile(context: Context, name: String = getName(context))
			= Shuttle(context, to = Users.parentProfile).launchNoThrows(with = Users.current()) { saveProfileName(this, it, name) }

	class NameInitializer: BroadcastReceiver() {

		override fun onReceive(context: Context, intent: Intent?) {
			if (intent?.action == Intent.ACTION_USER_INITIALIZE) setName(context, getDefaultName(context))
		}
	}
}
