package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.SpaceAppInput
import com.yzddmr6.prismspace.prism.compose.vm.SpaceSegment
import com.yzddmr6.prismspace.prism.compose.vm.mapRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests mapRows(), the pure mapper that drives Space screen cards.
 */
class SpaceMapTest {

    private fun dualInput(
        pkg: String = "com.example.app",
        label: String = "Test App",
        frozen: Boolean = false,
        suspended: Boolean = false,
        launchable: Boolean = true,
        system: Boolean = false,
        cloned: Boolean = false,
    ) = SpaceAppInput(
        pkg = pkg, label = label, frozen = frozen, suspended = suspended,
        launchable = launchable, system = system, cloned = cloned,
        segment = SpaceSegment.Dual,
    )

    private fun mainInput(
        pkg: String = "com.example.main",
        label: String = "Main App",
        frozen: Boolean = false,
        suspended: Boolean = false,
        launchable: Boolean = true,
        system: Boolean = false,
        cloned: Boolean = false,
    ) = SpaceAppInput(
        pkg = pkg, label = label, frozen = frozen, suspended = suspended,
        launchable = launchable, system = system, cloned = cloned,
        segment = SpaceSegment.Main,
    )

    // --- Dual segment ---

    @Test
    fun `dual frozen app gets chipText 已冻结 and chipOk false`() {
        val rows = mapRows(listOf(dualInput(frozen = true)))
        assertEquals(1, rows.size)
        assertEquals("已冻结", rows[0].chipText)
        assertFalse(rows[0].chipOk)
    }

    @Test
    fun `dual running app gets chipText 运行中 and chipOk true`() {
        val rows = mapRows(listOf(dualInput(frozen = false)))
        assertEquals(1, rows.size)
        assertEquals("运行中", rows[0].chipText)
        assertTrue(rows[0].chipOk)
    }

    @Test
    fun `dual frozen takes priority regardless of launchable`() {
        val rows = mapRows(listOf(dualInput(frozen = true, launchable = false)))
        assertEquals("已冻结", rows[0].chipText)
        assertFalse(rows[0].chipOk)
    }

    @Test
    fun `dual segment field preserved`() {
        val rows = mapRows(listOf(dualInput()))
        assertEquals(SpaceSegment.Dual, rows[0].segment)
    }

    // --- Main segment ---

    @Test
    fun `main cloned app gets chipText 已双开 and chipOk true`() {
        val rows = mapRows(listOf(mainInput(cloned = true)))
        assertEquals("已双开", rows[0].chipText)
        assertTrue(rows[0].chipOk)
    }

    @Test
    fun `main non-cloned app gets chipText 未双开 and chipOk false`() {
        val rows = mapRows(listOf(mainInput(cloned = false)))
        assertEquals("未双开", rows[0].chipText)
        assertFalse(rows[0].chipOk)
    }

    @Test
    fun `main segment field preserved`() {
        val rows = mapRows(listOf(mainInput()))
        assertEquals(SpaceSegment.Main, rows[0].segment)
    }

    // --- Field pass-through ---

    @Test
    fun `pkg and label pass through`() {
        val rows = mapRows(listOf(dualInput(pkg = "com.foo.bar", label = "Foo Bar")))
        assertEquals("com.foo.bar", rows[0].pkg)
        assertEquals("Foo Bar", rows[0].label)
    }

    @Test
    fun `frozen field passes through`() {
        val rows = mapRows(listOf(dualInput(frozen = true)))
        assertTrue(rows[0].frozen)
    }

    @Test
    fun `cloned field passes through`() {
        val rows = mapRows(listOf(mainInput(cloned = true)))
        assertTrue(rows[0].cloned)
    }

    @Test
    fun `empty list returns empty`() {
        val rows = mapRows(emptyList())
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `multiple apps mapped independently`() {
        val inputs = listOf(
            dualInput(pkg = "com.a", frozen = true),
            dualInput(pkg = "com.b", frozen = false),
            mainInput(pkg = "com.c", cloned = true),
        )
        val rows = mapRows(inputs)
        assertEquals(3, rows.size)
        assertEquals("已冻结", rows[0].chipText)
        assertEquals("运行中", rows[1].chipText)
        assertEquals("已双开", rows[2].chipText)
    }
}
