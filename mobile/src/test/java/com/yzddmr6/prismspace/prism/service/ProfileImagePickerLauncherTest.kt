package com.yzddmr6.prismspace.prism.service

import android.app.admin.DevicePolicyManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileImagePickerLauncherTest {

    @Test fun describesImageGetContentIntentForProfilePicker() {
        val spec = ProfileImagePickerLauncher.intentSpec()

        assertEquals(Intent.ACTION_GET_CONTENT, spec.action)
        assertEquals("image/*", spec.type)
        assertTrue(spec.categories.contains(Intent.CATEGORY_OPENABLE))
        assertTrue(spec.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun describesCrossProfileTrampolineIntentForManagedProfile() {
        val spec = ProfileImagePickerLauncher.crossProfileActivityIntentSpec()

        assertEquals("com.yzddmr6.prismspace.action.PROFILE_IMAGE_PICKER", spec.action)
        assertTrue(spec.categories.contains("com.yzddmr6.prismspace.category.MANAGED_PROFILE"))
        assertTrue(spec.categories.contains(Intent.CATEGORY_DEFAULT))
        assertEquals(
            DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT,
            ProfileImagePickerLauncher.crossProfileForwardingFlags(),
        )
        assertEquals(
            "com.yzddmr6.prismspace.prism.ui.ProfileImagePickerActivity",
            ProfileImagePickerLauncher.crossProfilePreferredActivityClassName(),
        )
    }

}
