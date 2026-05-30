package com.yzddmr6.prismspace.prism.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallSourcePermissionHelperTest {

    @Test fun plannerPrioritizesFileManagersBeforeDocumentsUi() {
        val source = InstallSourcePermissionHelper.firstSourceNeedingPermission(
            listOf(
                InstallSourcePermissionHelper.SourceState("com.google.android.documentsui", installed = true, declaresInstallPermission = true, canInstall = false),
                InstallSourcePermissionHelper.SourceState("com.android.fileexplorer", installed = true, declaresInstallPermission = true, canInstall = false),
                InstallSourcePermissionHelper.SourceState("bin.mt.plus", installed = true, declaresInstallPermission = true, canInstall = false),
            )
        )

        assertEquals("com.android.fileexplorer", source)
    }

    @Test fun plannerSkipsSourcesThatCannotBeAuthorized() {
        val source = InstallSourcePermissionHelper.firstSourceNeedingPermission(
            listOf(
                InstallSourcePermissionHelper.SourceState("com.google.android.documentsui", installed = true, declaresInstallPermission = false, canInstall = false),
                InstallSourcePermissionHelper.SourceState("com.android.fileexplorer", installed = false, declaresInstallPermission = true, canInstall = false),
            )
        )

        assertNull(source)
    }

    @Test fun plannerTreatsAnyAuthorizedKnownSourceAsReady() {
        val ready = InstallSourcePermissionHelper.anySourceReadyForInstall(
            listOf(
                InstallSourcePermissionHelper.SourceState("com.google.android.documentsui", installed = true, declaresInstallPermission = false, canInstall = false),
                InstallSourcePermissionHelper.SourceState("bin.mt.plus", installed = true, declaresInstallPermission = true, canInstall = true),
            )
        )

        assertTrue(ready)
    }

    @Test fun plannerDoesNotAskForAnotherSourceWhenOneSourceIsReady() {
        val source = InstallSourcePermissionHelper.sourceToRequestBeforeOpeningDownloads(
            listOf(
                InstallSourcePermissionHelper.SourceState("com.android.fileexplorer", installed = true, declaresInstallPermission = true, canInstall = false),
                InstallSourcePermissionHelper.SourceState("bin.mt.plus", installed = true, declaresInstallPermission = true, canInstall = true),
            )
        )

        assertNull(source)
    }

    @Test fun preferredSourceIsUsedWhenItIsActuallyInstalledAndNeedsPermission() {
        val source = InstallSourcePermissionHelper.sourceToRequestForPreferred(
            listOf(
                InstallSourcePermissionHelper.SourceState("com.android.fileexplorer", installed = true, declaresInstallPermission = true, canInstall = false),
                InstallSourcePermissionHelper.SourceState("bin.mt.plus", installed = true, declaresInstallPermission = true, canInstall = false),
            ),
            "com.android.fileexplorer",
        )

        assertEquals("com.android.fileexplorer", source)
    }

    @Test fun preferredSourceFallsBackWhenItIsMissing() {
        val source = InstallSourcePermissionHelper.sourceToRequestForPreferred(
            listOf(
                InstallSourcePermissionHelper.SourceState("com.android.fileexplorer", installed = false, declaresInstallPermission = true, canInstall = false),
                InstallSourcePermissionHelper.SourceState("bin.mt.plus", installed = true, declaresInstallPermission = true, canInstall = false),
            ),
            "com.android.fileexplorer",
        )

        assertEquals("bin.mt.plus", source)
    }
}
