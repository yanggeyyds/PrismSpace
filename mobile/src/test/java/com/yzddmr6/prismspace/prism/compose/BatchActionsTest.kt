package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.BatchAction
import com.yzddmr6.prismspace.prism.compose.vm.SpaceSegment
import com.yzddmr6.prismspace.prism.compose.vm.batchActionsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests batchActionsFor(), the pure function that determines
 * which batch actions appear in the batbar per segment.
 */
class BatchActionsTest {

    @Test
    fun `main segment has CopyToDual action`() {
        val actions = batchActionsFor(SpaceSegment.Main)
        assertTrue(actions.contains(BatchAction.CopyToDual))
    }

    @Test
    fun `main segment does not have Freeze action`() {
        val actions = batchActionsFor(SpaceSegment.Main)
        assertFalse(actions.contains(BatchAction.Freeze))
    }

    @Test
    fun `dual segment has Freeze action`() {
        val actions = batchActionsFor(SpaceSegment.Dual)
        assertTrue(actions.contains(BatchAction.Freeze))
    }

    @Test
    fun `dual segment has Uninstall action`() {
        val actions = batchActionsFor(SpaceSegment.Dual)
        assertTrue(actions.contains(BatchAction.Uninstall))
    }

    @Test
    fun `dual segment does not have CopyToDual action`() {
        val actions = batchActionsFor(SpaceSegment.Dual)
        assertFalse(actions.contains(BatchAction.CopyToDual))
    }

    @Test
    fun `main segment action list is non-empty`() {
        assertTrue(batchActionsFor(SpaceSegment.Main).isNotEmpty())
    }

    @Test
    fun `dual segment action list is non-empty`() {
        assertTrue(batchActionsFor(SpaceSegment.Dual).isNotEmpty())
    }
}
