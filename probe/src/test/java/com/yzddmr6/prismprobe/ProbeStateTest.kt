package com.yzddmr6.prismprobe

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeStateTest {

    @Test
    fun formatsIdentityLine() {
        val state = ProbeState(
            userId = 11,
            uid = 1101234,
            packageName = "com.yzddmr6.prismprobe",
            processName = "com.yzddmr6.prismprobe",
            sdkInt = 36,
            isManagedProfile = true,
        )

        assertEquals(
            "user=11 uid=1101234 package=com.yzddmr6.prismprobe process=com.yzddmr6.prismprobe sdk=36 managed=true",
            state.identityLine(),
        )
    }

    @Test
    fun summarizesFileVisibility() {
        val state = ProbeState(
            userId = 0,
            uid = 10234,
            packageName = "com.yzddmr6.prismprobe",
            processName = "com.yzddmr6.prismprobe",
            sdkInt = 36,
            isManagedProfile = false,
            visibleImageCount = 2,
            visibleDownloadCount = 3,
        )

        assertEquals("images=2 downloads=3", state.fileVisibilityLine())
    }

    @Test
    fun summarizesSensitivePermissionStatus() {
        val state = ProbeState(
            userId = 11,
            uid = 1101234,
            packageName = "com.yzddmr6.prismprobe",
            processName = "com.yzddmr6.prismprobe",
            sdkInt = 36,
            isManagedProfile = true,
            cameraGranted = true,
            microphoneGranted = false,
            locationGranted = false,
            contactsGranted = true,
            notificationGranted = false,
        )

        assertEquals(
            "camera=true microphone=false location=false contacts=true notification=false",
            state.permissionLine(),
        )
    }
}
