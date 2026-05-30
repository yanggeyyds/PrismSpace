package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.SpaceAppInput4
import com.yzddmr6.prismspace.prism.compose.vm.SpaceUiState
import com.yzddmr6.prismspace.prism.compose.vm.mapSpaceRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compat tests for the dual-profile stateText rules via mapSpaceRows().
 * The new authoritative tests live in SpaceMapTest.
 */
class SpaceUiStateTest {

    @Test
    fun `main and dual spaces show system apps by default`() {
        val state = SpaceUiState()

        assertTrue(state.showSystem)
        assertTrue(state.showSystemDual)
    }

    @Test
    fun `frozen dual app shows 已冻结`() {
        val input = listOf(SpaceAppInput4(pkg = "com.example.app", label = "My App", frozen = true, launchable = false))
        val rows = mapSpaceRows(input)
        assertEquals(1, rows.size)
        assertEquals("已冻结", rows[0].chipText)
    }

    @Test
    fun `non-frozen dual app shows 运行中`() {
        val input = listOf(SpaceAppInput4(pkg = "com.example.nolauncher", label = "No Launcher", frozen = false, launchable = false))
        val rows = mapSpaceRows(input)
        assertEquals(1, rows.size)
        assertEquals("运行中", rows[0].chipText)
    }

    @Test
    fun `normal dual app shows 运行中`() {
        val input = listOf(SpaceAppInput4(pkg = "com.example.normal", label = "Normal App", frozen = false, launchable = true))
        val rows = mapSpaceRows(input)
        assertEquals(1, rows.size)
        assertEquals("运行中", rows[0].chipText)
    }

    @Test
    fun `label and pkg pass through correctly`() {
        val input = listOf(SpaceAppInput4(pkg = "com.example.passthrough", label = "Pass Through", frozen = false, launchable = true))
        val rows = mapSpaceRows(input)
        assertEquals(1, rows.size)
        assertEquals("com.example.passthrough", rows[0].pkg)
        assertEquals("Pass Through", rows[0].label)
    }

    @Test
    fun `frozen takes priority over not-launchable`() {
        val input = listOf(SpaceAppInput4(pkg = "com.example.frozennolaunch", label = "Frozen No Launch", frozen = true, launchable = false))
        val rows = mapSpaceRows(input)
        assertEquals(1, rows.size)
        assertEquals("已冻结", rows[0].chipText)
    }
}
