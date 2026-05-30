package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.space.CREATE_CHIP_ID
import com.yzddmr6.prismspace.prism.compose.space.PrismSpace
import com.yzddmr6.prismspace.prism.compose.space.PrismSpaceKind
import com.yzddmr6.prismspace.prism.compose.space.SpaceChip
import com.yzddmr6.prismspace.prism.compose.space.pickDefaultDualSpaceId
import com.yzddmr6.prismspace.prism.compose.space.spaceChips
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceChipsTest {
    private fun main() = PrismSpace("main", 0, PrismSpaceKind.Main, "主空间")
    private fun dual(uid: Int, name: String) = PrismSpace("space_$uid", uid, PrismSpaceKind.Dual, name)

    @Test fun `default dual id is the first dual, null when none`() {
        assertEquals("space_10", pickDefaultDualSpaceId(listOf(main(), dual(10, "双开空间"))))
        assertEquals("space_11", pickDefaultDualSpaceId(listOf(main(), dual(11, "A"), dual(13, "B"))))
        assertNull(pickDefaultDualSpaceId(listOf(main())))
    }

    @Test fun `chips = main + each dual + trailing create, selection flags correct`() {
        val spaces = listOf(main(), dual(11, "双A"), dual(13, "双B"))
        val chips = spaceChips(spaces, selectedMain = false, selectedDualId = "space_13", showCreate = true)
        assertEquals(
            listOf("main" to "主空间", "space_11" to "双A", "space_13" to "双B", CREATE_CHIP_ID to "＋新建空间"),
            chips.map { it.id to it.label },
        )
        assertTrue(chips.first { it.id == "space_13" }.selected)
        assertTrue(chips.none { it.id == "main" && it.selected })
        assertTrue(chips.first { it.id == CREATE_CHIP_ID }.isCreate)
        assertTrue(chips.none { it.isCreate && it.selected })
    }

    @Test fun `main selected highlights only main`() {
        val chips = spaceChips(listOf(main(), dual(11, "双A")), selectedMain = true, selectedDualId = null)
        assertTrue(chips.first { it.id == "main" }.selected)
        assertTrue(chips.none { it.id == "space_11" && it.selected })
    }

    @Test fun `stale selectedDualId selects nothing`() {
        val chips = spaceChips(listOf(main(), dual(11, "双A")), selectedMain = false, selectedDualId = "space_99")
        assertTrue(chips.none { it.selected })
    }

    @Test fun `create chip hidden by default`() {
        val chips = spaceChips(listOf(main(), dual(11, "A")), false, null)
        assertTrue(chips.none { it.isCreate })
    }

    @Test fun `create chip shown when enabled`() {
        val chips = spaceChips(listOf(main(), dual(11, "A")), false, null, showCreate = true)
        assertEquals(CREATE_CHIP_ID, chips.last().id)
        assertTrue(chips.last().isCreate)
    }
}
