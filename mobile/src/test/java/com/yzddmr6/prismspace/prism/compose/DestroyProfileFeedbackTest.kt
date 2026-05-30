package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.DestroyFeedback
import com.yzddmr6.prismspace.prism.compose.vm.destroyProfileFeedback
import com.yzddmr6.prismspace.setup.DestroyProfileResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure contract for the destroy-profile feedback mapper (no Android deps). */
class DestroyProfileFeedbackTest {
    @Test fun `success is benign, never an error, no system route`() {
        val f = destroyProfileFeedback(DestroyProfileResult.Success)
        assertEquals("正在删除双开空间…", f.message)
        assertFalse(f.isError)
        assertFalse(f.routeToSystemRemoval)
    }

    @Test fun `not profile owner routes to system removal as an error`() {
        val f = destroyProfileFeedback(DestroyProfileResult.NotProfileOwner)
        assertEquals("无管理权限，需在系统设置中手动移除双开空间", f.message)
        assertTrue(f.isError)
        assertTrue(f.routeToSystemRemoval)
    }

    @Test fun `failed with reason is a retryable error, no system route`() {
        val f = destroyProfileFeedback(DestroyProfileResult.Failed("boom"))
        assertEquals("删除失败：boom，空间未被破坏，可重试", f.message)
        assertTrue(f.isError)
        assertFalse(f.routeToSystemRemoval)
    }

    @Test fun `failed with null reason falls back to a generic cause`() {
        val f = destroyProfileFeedback(DestroyProfileResult.Failed(null))
        assertEquals("删除失败：未知错误，空间未被破坏，可重试", f.message)
        assertTrue(f.isError)
        assertFalse(f.routeToSystemRemoval)
    }

    @Test fun `failed with blank reason also falls back to a generic cause`() {
        val f = destroyProfileFeedback(DestroyProfileResult.Failed("   "))
        assertEquals("删除失败：未知错误，空间未被破坏，可重试", f.message)
        assertTrue(f.isError)
        assertFalse(f.routeToSystemRemoval)
    }
}
