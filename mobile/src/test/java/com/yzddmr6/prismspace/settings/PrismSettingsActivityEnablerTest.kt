package com.yzddmr6.prismspace.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PrismSettingsActivityEnablerTest {

    @Test fun enablerAcceptsUserInitializeBecauseManifestRegistersIt() {
        val source = File("src/main/java/com/yzddmr6/prismspace/settings/PrismSettingsActivity.kt").readText()

        assertTrue(source.contains("Intent.ACTION_USER_INITIALIZE"))
        assertTrue(source.contains("intent.action != Intent.ACTION_USER_INITIALIZE"))
    }
}
