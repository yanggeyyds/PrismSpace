package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.space.PrismSpace
import com.yzddmr6.prismspace.prism.compose.space.PrismSpaceKind
import com.yzddmr6.prismspace.prism.compose.space.pickDefaultDualSpaceId
import com.yzddmr6.prismspace.prism.compose.space.selectedDualChipId
import com.yzddmr6.prismspace.prism.compose.vm.SpaceSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedDualChipIdTest {
    private fun main() = PrismSpace("main", 0, PrismSpaceKind.Main, "主空间")
    private fun dual(id: String, uid: Int, name: String) = PrismSpace(id, uid, PrismSpaceKind.Dual, name)

    @Test fun `main segment selects no dual chip`() {
        val spaces = listOf(main(), dual("d1", 10, "双开空间"))

        assertNull(selectedDualChipId(SpaceSegment.Main, "d1", spaces))
    }

    @Test fun `dual segment keeps selected dual chip`() {
        val spaces = listOf(main(), dual("d1", 10, "双开空间"))

        assertEquals("d1", selectedDualChipId(SpaceSegment.Dual, "d1", spaces))
    }

    @Test fun `dual segment falls back to default dual chip`() {
        val spaces = listOf(main(), dual("d1", 10, "双开空间"), dual("d2", 11, "双开空间 2"))

        assertEquals(pickDefaultDualSpaceId(spaces), selectedDualChipId(SpaceSegment.Dual, null, spaces))
    }
}
