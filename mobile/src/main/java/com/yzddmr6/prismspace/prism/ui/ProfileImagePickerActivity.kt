package com.yzddmr6.prismspace.prism.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.yzddmr6.prismspace.prism.service.ProfileImagePickerLauncher

class ProfileImagePickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ProfileImagePickerLauncher.open(this)
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to open profile image picker", e)
        } finally {
            finish()
        }
    }

    private companion object {
        private const val TAG = "Prism.ProfilePicker"
    }
}
