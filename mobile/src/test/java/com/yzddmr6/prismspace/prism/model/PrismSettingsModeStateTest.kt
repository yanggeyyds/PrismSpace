package com.yzddmr6.prismspace.prism.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PrismSettingsModeStateTest {

    @Test fun normalModeIsAlwaysAvailableForCoreDualOpen() {
        val state = PrismSettingsModeState.from(
            shizuku = PrismShizukuAdbStatus.NotInstalled,
            root = PrismRootStatus.NotDetected,
        )

        assertEquals("普通模式", state.normal.title)
        assertEquals("可用", state.normal.status)
        assertEquals("双开空间、应用打开、冻结、暂停和基础文件导入导出不需要 Root。", state.normal.summary)
    }

    @Test fun shizukuAdbDistinguishesSetupStates() {
        assertEquals(
            "未安装",
            PrismSettingsModeState.from(PrismShizukuAdbStatus.NotInstalled, PrismRootStatus.NotDetected).shizukuAdb.status,
        )
        assertEquals(
            "未运行",
            PrismSettingsModeState.from(PrismShizukuAdbStatus.NotRunning, PrismRootStatus.NotDetected).shizukuAdb.status,
        )
        assertEquals(
            "等待授权",
            PrismSettingsModeState.from(PrismShizukuAdbStatus.WaitingAuthorization, PrismRootStatus.NotDetected).shizukuAdb.status,
        )
        assertEquals(
            "可用",
            PrismSettingsModeState.from(PrismShizukuAdbStatus.Ready, PrismRootStatus.NotDetected).shizukuAdb.status,
        )
    }

    @Test fun dhizukuDistinguishesSetupStates() {
        assertEquals(
            "未安装",
            PrismSettingsModeState.from(
                PrismShizukuAdbStatus.NotInstalled, PrismRootStatus.NotDetected, PrismDhizukuStatus.NotInstalled).dhizuku.status,
        )
        assertEquals(
            "未激活",
            PrismSettingsModeState.from(
                PrismShizukuAdbStatus.NotInstalled, PrismRootStatus.NotDetected, PrismDhizukuStatus.NotActivated).dhizuku.status,
        )
        assertEquals(
            "等待授权",
            PrismSettingsModeState.from(
                PrismShizukuAdbStatus.NotInstalled, PrismRootStatus.NotDetected, PrismDhizukuStatus.WaitingAuthorization).dhizuku.status,
        )
        assertEquals(
            "可用",
            PrismSettingsModeState.from(
                PrismShizukuAdbStatus.NotInstalled, PrismRootStatus.NotDetected, PrismDhizukuStatus.Ready).dhizuku.status,
        )
    }

    @Test fun dhizukuDefaultsToNotActivatedWhenOmitted() {
        // When the caller doesn't specify a Dhizuku status, the default is NotActivated ("未激活").
        val state = PrismSettingsModeState.from(
            PrismShizukuAdbStatus.NotInstalled, PrismRootStatus.NotDetected)
        assertEquals("未激活", state.dhizuku.status)
    }

    @Test fun rootModeCopySaysItIsFallbackOnly() {
        val state = PrismSettingsModeState.from(
            shizuku = PrismShizukuAdbStatus.Ready,
            root = PrismRootStatus.AvailableButDisabled,
        )

        assertEquals("Root 模式", state.root.title)
        assertEquals("可用但关闭", state.root.status)
        assertEquals("Root 仅用于高级诊断和兜底维护，不是普通双开功能的依赖。", state.root.summary)
    }
}
