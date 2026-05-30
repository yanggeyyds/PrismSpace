package com.yzddmr6.prismspace.shuttle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuttleBoundedTest {
    @Test fun fastBlockReturnsValue() {
        assertEquals("ok", runBounded(2_000L) { "ok" })
    }
    @Test fun nullValueInTimeIsNull() {
        assertNull(runBounded(2_000L) { null })
    }
    @Test fun throwingBlockRethrows() {
        try { runBounded(2_000L) { throw IllegalStateException("boom") }; throw AssertionError("no throw") }
        catch (e: IllegalStateException) { assertEquals("boom", e.message) }
    }
    @Test fun slowBlockTimesOutNullAndBounded() {
        val t0 = System.currentTimeMillis()
        val r = runBounded(300L) { Thread.sleep(3_000L); "late" }
        val elapsed = System.currentTimeMillis() - t0
        assertNull(r)
        assertTrue("elapsed=$elapsed should be < ~1500ms", elapsed < 1_500L)
    }
}
