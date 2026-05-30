package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.data.PrismAppInfo
import com.yzddmr6.prismspace.prism.compose.space.PrismSpace
import com.yzddmr6.prismspace.prism.compose.space.PrismSpaceKind
import com.yzddmr6.prismspace.prism.compose.space.SpaceRepository
import com.yzddmr6.prismspace.prism.compose.space.SpaceUsability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure SpaceRepository contract at N=0 / N=1 / N≥2 (no Android deps via FakeSpaceRepository). */
class SpaceRepositoryTest {
    /** In-memory fake: `dualUserIds` = the managed-profile userIds (empty ⇒ N=0). */
    private class FakeSpaceRepository(private val dualUserIds: List<Int>) : SpaceRepository {
        override fun spaces(): List<PrismSpace> = buildList {
            add(PrismSpace("main", 0, PrismSpaceKind.Main, "主空间"))
            dualUserIds.sorted().forEach { uid ->
                // Real names come from PrismNameManager (resource strings), not a "$uid"
                // suffix; this Fake uses a simple suffix only to keep N>=2 spaces
                // distinguishable in assertions — do NOT assert displayName on N>=2 here.
                add(PrismSpace("space_$uid", uid, PrismSpaceKind.Dual,
                    if (dualUserIds.size == 1) "双开空间" else "双开空间$uid"))
            }
        }
        override fun mainSpace() = spaces().first { it.kind == PrismSpaceKind.Main }
        override fun dualSpace() = dualSpaces().firstOrNull()
        override fun dualSpaces() = spaces().filter { it.kind == PrismSpaceKind.Dual }
        override fun space(id: String) = spaces().firstOrNull { it.id == id }
        override fun usabilityOf(space: PrismSpace) = SpaceUsability.Usable
        override fun installedApps(space: PrismSpace) = emptyList<PrismAppInfo>()
        override fun cloneTargetSpaceCount() = 1 + dualUserIds.size
    }

    @Test fun `N=0 has only main, no dual`() {
        val r = FakeSpaceRepository(emptyList())
        assertEquals(1, r.spaces().size)
        assertEquals(PrismSpaceKind.Main, r.spaces()[0].kind)
        assertNull(r.dualSpace())
        assertTrue(r.dualSpaces().isEmpty())
        assertEquals(1, r.cloneTargetSpaceCount())
        assertNull(r.space("space_5"))
    }

    @Test fun `N=1 has main plus dual - counts match exposed size semantics`() {
        val r = FakeSpaceRepository(listOf(10))
        assertEquals(2, r.spaces().size)
        assertEquals("双开空间", r.dualSpace()!!.displayName)
        assertEquals(10, r.dualSpace()!!.userId)
        assertEquals("space_10", r.dualSpace()!!.id)
        assertEquals(r.dualSpace(), r.space("space_10"))     // id round-trips via space(id)
        assertEquals("主空间", r.mainSpace().displayName)
        assertEquals("main", r.mainSpace().id)
        assertEquals(1, r.dualSpaces().size)
        assertEquals(2, r.cloneTargetSpaceCount())
        assertFalse(r.cloneTargetSpaceCount() > 2)
        assertTrue(r.cloneTargetSpaceCount() <= 2)
    }

    @Test fun `N=2 enumerates main plus both duals, ordered, looked-up by id`() {
        val r = FakeSpaceRepository(listOf(13, 11))
        assertEquals(3, r.spaces().size)
        assertEquals(listOf(0, 11, 13), r.spaces().map { it.userId })
        assertEquals(2, r.dualSpaces().size)
        assertEquals(listOf(11, 13), r.dualSpaces().map { it.userId })
        assertEquals(11, r.dualSpace()!!.userId)
        assertEquals(3, r.cloneTargetSpaceCount())
        assertTrue(r.cloneTargetSpaceCount() > 2)
        assertEquals(13, r.space("space_13")!!.userId)
        assertEquals(PrismSpaceKind.Dual, r.space("space_11")!!.kind)
        assertNull(r.space("space_99"))
        assertEquals("main", r.mainSpace().id)
    }
}
