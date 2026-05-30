package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.CloneFilter
import com.yzddmr6.prismspace.prism.compose.vm.SortOrder
import com.yzddmr6.prismspace.prism.compose.vm.SpaceAppInput
import com.yzddmr6.prismspace.prism.compose.vm.SpaceRow
import com.yzddmr6.prismspace.prism.compose.vm.SpaceSegment
import com.yzddmr6.prismspace.prism.compose.vm.applyListTransform
import com.yzddmr6.prismspace.prism.compose.vm.mapRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests client-side filter, sort and search logic in applyListTransform().
 */
class SpaceFilterSortTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun makeRow(
        pkg: String,
        label: String,
        system: Boolean = false,
        cloned: Boolean = false,
        segment: SpaceSegment = SpaceSegment.Main,
        loadIndex: Int = 0,
    ): SpaceRow {
        val input = SpaceAppInput(
            pkg = pkg,
            label = label,
            frozen = false,
            suspended = false,
            launchable = true,
            system = system,
            cloned = cloned,
            segment = segment,
        )
        return mapRows(listOf(input))[0].copy()
    }

    private val wechat = makeRow("com.tencent.mm", "微信", loadIndex = 0)
    private val twitter = makeRow("com.twitter.android", "X", cloned = true, loadIndex = 1)
    private val camera = makeRow("com.android.camera", "相机", system = true, loadIndex = 2)
    private val alipay = makeRow("com.eg.android.AlipayGphone", "支付宝", loadIndex = 3)
    private val telegram = makeRow("org.telegram.messenger", "Telegram", cloned = true, loadIndex = 4)

    private val mainRows = listOf(wechat, twitter, camera, alipay, telegram)

    private val dualX = makeRow("com.twitter.android", "X", segment = SpaceSegment.Dual, loadIndex = 0)
    private val dualTelegram = makeRow("org.telegram.messenger", "Telegram", segment = SpaceSegment.Dual, loadIndex = 1)
    private val dualRows = listOf(dualX, dualTelegram)

    // -----------------------------------------------------------------------
    // Search / keyword matching
    // -----------------------------------------------------------------------

    @Test
    fun `keyword match on label case-insensitive`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "wechat",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        // "wechat" matches neither the Chinese label nor the package name.
        assertTrue(result.isEmpty()) // "wechat" matches neither label nor pkg
    }

    @Test
    fun `keyword match on label`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "Telegram",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals(1, result.size)
        assertEquals("org.telegram.messenger", result[0].pkg)
    }

    @Test
    fun `keyword match is case-insensitive`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "telegram",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals(1, result.size)
        assertEquals("org.telegram.messenger", result[0].pkg)
    }

    @Test
    fun `keyword match on packageName`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "tencent",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals(1, result.size)
        assertEquals("com.tencent.mm", result[0].pkg)
    }

    @Test
    fun `empty query returns all rows`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals(mainRows.size, result.size)
    }

    // -----------------------------------------------------------------------
    // Filter: hide system apps
    // -----------------------------------------------------------------------

    @Test
    fun `hide system apps when showSystem=false`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = false,
        )
        assertTrue(result.none { it.system })
        assertTrue(result.none { it.pkg == "com.android.camera" })
    }

    @Test
    fun `show system apps when showSystem=true`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertTrue(result.any { it.pkg == "com.android.camera" })
    }

    @Test
    fun `showSystem filter applied for dual segment too`() {
        // The dual segment honours its own «显示系统应用» toggle. Both segments default ON, while OFF
        // still hides launchable system apps for the current segment.
        val dualWithSystem = dualRows + makeRow("com.android.system", "System", system = true, segment = SpaceSegment.Dual)
        val hidden = applyListTransform(
            rows = dualWithSystem,
            segment = SpaceSegment.Dual,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = false,
        )
        assertTrue(hidden.none { it.system })
        assertEquals(dualRows.size, hidden.size)

        val shown = applyListTransform(
            rows = dualWithSystem,
            segment = SpaceSegment.Dual,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals(dualWithSystem.size, shown.size)
    }

    // -----------------------------------------------------------------------
    // Filter: cloneFilter (main segment only)
    // -----------------------------------------------------------------------

    @Test
    fun `filter only-cloned returns only cloned apps`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.Yes,
            showSystem = true,
        )
        assertTrue(result.all { it.cloned })
        assertEquals(2, result.size) // twitter + telegram are cloned
    }

    @Test
    fun `filter only-not-cloned returns only non-cloned apps`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.No,
            showSystem = true,
        )
        assertTrue(result.none { it.cloned })
    }

    @Test
    fun `filter all returns all apps`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals(mainRows.size, result.size)
    }

    @Test
    fun `clone filter not applied for dual segment`() {
        val result = applyListTransform(
            rows = dualRows,
            segment = SpaceSegment.Dual,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.Yes, // should be ignored for dual
            showSystem = true,
        )
        assertEquals(dualRows.size, result.size)
    }

    // -----------------------------------------------------------------------
    // Sort: name
    // -----------------------------------------------------------------------

    @Test
    fun `name sort orders labels alphabetically`() {
        val rows = listOf(
            makeRow("com.b", "Banana"),
            makeRow("com.a", "Apple"),
            makeRow("com.c", "Cherry"),
        )
        val result = applyListTransform(
            rows = rows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals("Apple", result[0].label)
        assertEquals("Banana", result[1].label)
        assertEquals("Cherry", result[2].label)
    }

    // -----------------------------------------------------------------------
    // Sort: time (uses stable load order via loadIndex, most-recent-first)
    // -----------------------------------------------------------------------

    @Test
    fun `time sort uses load index descending (most-recent-first)`() {
        // loadIndex is assigned by position in the rows list; higher index = more recently loaded
        val rows = listOf(
            makeRow("com.first", "First", loadIndex = 0),
            makeRow("com.second", "Second", loadIndex = 1),
            makeRow("com.third", "Third", loadIndex = 2),
        )
        val result = applyListTransform(
            rows = rows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Time,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        // Most recent (highest load index = last in list) should come first
        assertEquals("com.third", result[0].pkg)
        assertEquals("com.second", result[1].pkg)
        assertEquals("com.first", result[2].pkg)
    }

    // -----------------------------------------------------------------------
    // Sort: cloned (已双开 first; main segment only)
    // -----------------------------------------------------------------------

    @Test
    fun `cloned sort puts cloned apps first in main segment`() {
        val result = applyListTransform(
            rows = mainRows,
            segment = SpaceSegment.Main,
            query = "",
            sort = SortOrder.Cloned,
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        val clonedCount = result.takeWhile { it.cloned }.size
        val nonClonedStarted = result.dropWhile { it.cloned }.any { it.cloned }
        assertTrue(clonedCount >= 2)
        assertTrue(!nonClonedStarted)
    }

    @Test
    fun `cloned sort falls back to name sort in dual segment`() {
        val rows = listOf(
            makeRow("com.b", "Banana", segment = SpaceSegment.Dual),
            makeRow("com.a", "Apple", segment = SpaceSegment.Dual),
        )
        val result = applyListTransform(
            rows = rows,
            segment = SpaceSegment.Dual,
            query = "",
            sort = SortOrder.Cloned, // not applicable for dual, should fall back to name
            cloneFilter = CloneFilter.All,
            showSystem = true,
        )
        assertEquals("Apple", result[0].label)
    }

    // -----------------------------------------------------------------------
    // Segment-appropriateness: filters & cloned-sort only apply to main
    // -----------------------------------------------------------------------

    @Test
    fun `dual segment ignores cloneFilter but honours showSystem`() {
        val rowsWithSystem = dualRows + makeRow("com.sys", "Sys", system = true, segment = SpaceSegment.Dual)
        val result = applyListTransform(
            rows = rowsWithSystem,
            segment = SpaceSegment.Dual,
            query = "",
            sort = SortOrder.Name,
            cloneFilter = CloneFilter.Yes,   // #1: cloneFilter stays main-only → ignored for dual
            showSystem = false,              // #1: showSystem now applies to dual → system row hidden
        )
        assertTrue(result.none { it.system })    // showSystem honoured (system removed)
        assertEquals(dualRows.size, result.size) // cloneFilter ignored → all non-system dual rows kept
    }
}
