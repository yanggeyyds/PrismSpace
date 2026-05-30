package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.component.PrismLevel
import com.yzddmr6.prismspace.prism.compose.vm.PrismMode
import com.yzddmr6.prismspace.prism.compose.vm.mapSettingsUiModel
import com.yzddmr6.prismspace.prism.model.CapabilityAvailability
import com.yzddmr6.prismspace.prism.model.CapabilityState
import com.yzddmr6.prismspace.prism.model.PrismRootStatus
import com.yzddmr6.prismspace.prism.model.PrismSettingsModeState
import com.yzddmr6.prismspace.prism.model.PrismShizukuAdbStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the pure mapper mapSettingsUiModel().
 */
class SettingsMapTest {

    private fun modeState(shizuku: PrismShizukuAdbStatus) = PrismSettingsModeState.from(
        shizuku = shizuku,
        root = PrismRootStatus.NotDetected,
    )

    private fun capabilityState(
        profileOwner: Boolean = true,
        shizukuReady: Boolean = false,
        shizukuAvailable: Boolean = shizukuReady,
        rootEnabled: Boolean = false,
    ) = CapabilityState(
        normal = CapabilityAvailability.Available,
        shizuku = when {
            shizukuReady -> CapabilityAvailability.Available
            shizukuAvailable -> CapabilityAvailability.NeedsSetup("Shizuku 等待授权")
            else -> CapabilityAvailability.NeedsSetup("Shizuku 未连接")
        },
        root = when {
            rootEnabled -> CapabilityAvailability.Available
            else -> CapabilityAvailability.Unsupported("Root 不可用")
        },
        profileOwner = if (profileOwner) CapabilityAvailability.Available
        else CapabilityAvailability.NeedsSetup("双开空间未创建"),
    )

    // -----------------------------------------------------------------------
    // modeTitle is always "当前模式"
    // -----------------------------------------------------------------------
    @Test
    fun `modeTitle is always 当前模式`() {
        val model = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = false,
            shizukuAvailable = false,
            modeState = modeState(PrismShizukuAdbStatus.NotRunning),
            capabilityState = capabilityState(),
            selectedMode = PrismMode.Normal,
        )
        assertEquals("当前模式", model.modeTitle)
    }

    // -----------------------------------------------------------------------
    // profileOwner=false -> level Error, body mentions 未创建
    // -----------------------------------------------------------------------
    @Test
    fun `profileOwner false - level Error and body mentions 未创建`() {
        val model = mapSettingsUiModel(
            profileOwner = false,
            shizukuAuthorized = false,
            shizukuAvailable = false,
            modeState = modeState(PrismShizukuAdbStatus.NotRunning),
            capabilityState = capabilityState(profileOwner = false),
            selectedMode = PrismMode.Normal,
        )
        assertEquals(PrismLevel.Error, model.level)
        assertTrue("body should mention 未创建", model.modeBody.contains("未创建"))
        assertFalse(model.profileOwnerReady)
    }

    // -----------------------------------------------------------------------
    // shizukuAuthorized=true -> level Ok, body mentions Shizuku/ADB
    // -----------------------------------------------------------------------
    @Test
    fun `shizukuAuthorized true - level Ok and body mentions Shizuku`() {
        val model = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = true,
            shizukuAvailable = true,
            modeState = modeState(PrismShizukuAdbStatus.Ready),
            capabilityState = capabilityState(profileOwner = true, shizukuReady = true),
            selectedMode = PrismMode.Shizuku,
        )
        assertEquals(PrismLevel.Ok, model.level)
        assertTrue("body should mention Shizuku", model.modeBody.contains("Shizuku"))
        assertTrue(model.profileOwnerReady)
    }

    // -----------------------------------------------------------------------
    // Normal mode isActive=true when selectedMode=Normal
    // -----------------------------------------------------------------------
    @Test
    fun `normal mode is always active`() {
        val model = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = false,
            shizukuAvailable = false,
            modeState = modeState(PrismShizukuAdbStatus.NotRunning),
            capabilityState = capabilityState(),
            selectedMode = PrismMode.Normal,
        )
        assertTrue("Normal mode should be active when selectedMode=Normal", model.normalMode.isActive)
        assertEquals("普通模式", model.normalMode.title)
    }

    // -----------------------------------------------------------------------
    // Shizuku/ADB mode isActive only when selected and available
    // -----------------------------------------------------------------------
    @Test
    fun `shizukuAdb mode isActive when shizuku capability Available`() {
        val active = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = true,
            shizukuAvailable = true,
            modeState = modeState(PrismShizukuAdbStatus.Ready),
            capabilityState = capabilityState(shizukuReady = true),
            selectedMode = PrismMode.Shizuku,
        )
        assertTrue(active.shizukuAdbMode.isActive)

        val inactive = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = false,
            shizukuAvailable = false,
            modeState = modeState(PrismShizukuAdbStatus.NotRunning),
            capabilityState = capabilityState(shizukuReady = false, shizukuAvailable = false),
            selectedMode = PrismMode.Normal,
        )
        assertFalse(inactive.shizukuAdbMode.isActive)
    }

    // -----------------------------------------------------------------------
    // Root mode isActive only when selected and available
    // -----------------------------------------------------------------------
    @Test
    fun `root mode isActive when root Available`() {
        val withRoot = capabilityState(rootEnabled = true).copy(
            root = CapabilityAvailability.Available,
        )
        val active = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = false,
            shizukuAvailable = false,
            modeState = modeState(PrismShizukuAdbStatus.NotRunning),
            capabilityState = withRoot,
            selectedMode = PrismMode.Root,
        )
        assertTrue(active.rootMode.isActive)

        val inactive = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = false,
            shizukuAvailable = false,
            modeState = modeState(PrismShizukuAdbStatus.NotRunning),
            capabilityState = capabilityState(rootEnabled = false),
            selectedMode = PrismMode.Normal,
        )
        assertFalse(inactive.rootMode.isActive)
    }

    // -----------------------------------------------------------------------
    // shizukuAdb status label reflects PrismSettingsModeState status (capability label)
    // Mirrors PrismSettingsModeState.from() shizuku label propagation
    // -----------------------------------------------------------------------
    @Test
    fun `shizukuAdb status label matches PrismShizukuAdbStatus label`() {
        val waitingModel = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = false,
            shizukuAvailable = true,
            modeState = modeState(PrismShizukuAdbStatus.WaitingAuthorization),
            capabilityState = capabilityState(shizukuAvailable = true, shizukuReady = false),
            selectedMode = PrismMode.Normal,
        )
        assertEquals("等待授权", waitingModel.shizukuAdbMode.statusLabel)

        val readyModel = mapSettingsUiModel(
            profileOwner = true,
            shizukuAuthorized = true,
            shizukuAvailable = true,
            modeState = modeState(PrismShizukuAdbStatus.Ready),
            capabilityState = capabilityState(shizukuReady = true),
            selectedMode = PrismMode.Shizuku,
        )
        assertEquals("可用", readyModel.shizukuAdbMode.statusLabel)
    }
}
