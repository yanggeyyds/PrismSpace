package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.engine.LaunchResult
import com.yzddmr6.prismspace.prism.compose.vm.LaunchFeedback
import com.yzddmr6.prismspace.prism.compose.vm.launchFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchFeedbackTest {
    @Test fun `ok yields no message, not error`() {
        val f = launchFeedback(LaunchResult.Ok, "微博")
        assertEquals("", f.message)
        assertFalse(f.isError)
    }
    @Test fun `space not ready is actionable error`() {
        val f = launchFeedback(LaunchResult.SpaceNotReady, "微博")
        assertEquals("双开空间尚未就绪，请在设置中检查空间状态或权限模式", f.message)
        assertTrue(f.isError)
    }
    @Test fun `app missing names the app`() {
        val f = launchFeedback(LaunchResult.AppMissing, "微博")
        assertEquals("微博 在该空间没有可启动入口", f.message)
        assertTrue(f.isError)
    }
    @Test fun `denied is a permission error`() {
        val f = launchFeedback(LaunchResult.Denied, "微博")
        assertEquals("无权限访问该空间，请检查权限模式", f.message)
        assertTrue(f.isError)
    }
    @Test fun `unknown carries reason, null-and-blank fall back`() {
        assertEquals("未能启动 微博：boom", launchFeedback(LaunchResult.Unknown("boom"), "微博").message)
        assertEquals("未能启动 微博：未知错误", launchFeedback(LaunchResult.Unknown(null), "微博").message)
        assertEquals("未能启动 微博：未知错误", launchFeedback(LaunchResult.Unknown("  "), "微博").message)
        assertTrue(launchFeedback(LaunchResult.Unknown(null), "微博").isError)
    }
}
