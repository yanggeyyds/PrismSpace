package com.yzddmr6.prismspace.prism.service

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CrossProfilePermissionManifestTest {

    @Test fun appDeclaresInteractAcrossProfilesForDirectProfileEntryLaunch() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.INTERACT_ACROSS_PROFILES"))
    }
}
