package com.yzddmr6.prismspace.prism.service

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

class ProfileSystemAppsProvisioningGuardTest {

    @Test
    fun provisioningKeepsUserFacingInstallEntrancesEnabled() {
        val source = String(Files.readAllBytes(systemAppsManagerSource()), StandardCharsets.UTF_8)

        assertTrue(source.contains("\"com.miui.packageinstaller\""))
        assertTrue(source.contains("\"com.android.fileexplorer\""))
        assertTrue(source.contains("\"com.google.android.documentsui\""))
    }

    @Test
    fun incrementalProvisioningAlsoRepairsCriticalSystemApps() {
        val source = String(Files.readAllBytes(prismProvisioningSource()), StandardCharsets.UTF_8)
        val method = source.substringAfter("performIncrementalProfileOwnerProvisioningIfNeeded")
            .substringBefore("public static")

        assertTrue(method.contains("enableCriticalAppsIfNeeded(context, policies)"))
    }

    @Test
    fun freshProvisioningUnhidesCriticalSystemAppsAfterSystemAppPruning() {
        val source = String(Files.readAllBytes(prismProvisioningSource()), StandardCharsets.UTF_8)
        val freshProvisioning = source.substringAfter("final SharedPreferences prefs")
            .substringBefore("setupLauncherActivityInPrism(context)")

        val pruneIndex = freshProvisioning.indexOf("hideUnnecessaryAppsInManagedProfile(context)")
        val criticalIndex = freshProvisioning.lastIndexOf("enableCriticalAppsIfNeeded(context, policies)")

        assertTrue(pruneIndex >= 0)
        assertTrue(criticalIndex > pruneIndex)
    }

    private fun systemAppsManagerSource(): Path {
        return sourcePath("shared/src/main/java/com/yzddmr6/prismspace/provisioning/SystemAppsManager.java")
    }

    private fun prismProvisioningSource(): Path {
        return sourcePath("engine/src/main/java/com/yzddmr6/prismspace/provisioning/PrismProvisioning.java")
    }

    private fun sourcePath(relativePath: String): Path {
        var current = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        while (current != null) {
            val source = current.resolve(relativePath)
            if (Files.exists(source)) return source
            current = current.parent
        }
        error("Cannot locate $relativePath from ${System.getProperty("user.dir")}")
    }
}
