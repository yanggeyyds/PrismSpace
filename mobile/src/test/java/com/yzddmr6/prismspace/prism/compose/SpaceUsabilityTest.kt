package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.space.SpaceUsability
import com.yzddmr6.prismspace.prism.compose.space.spaceUsability
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceUsabilityTest {
    @Test fun `not provisioned wins over running and unlocked inputs`() {
        assertEquals(SpaceUsability.NotProvisioned, spaceUsability(false, false, false))
        assertEquals(SpaceUsability.NotProvisioned, spaceUsability(false, true, true))
    }

    @Test fun `provisioned but not running is suspended`() {
        assertEquals(SpaceUsability.Suspended, spaceUsability(true, false, false))
        assertEquals(SpaceUsability.Suspended, spaceUsability(true, false, true))
    }

    @Test fun `running but CE locked needs unlock`() {
        assertEquals(SpaceUsability.LockedNeedsUnlock, spaceUsability(true, true, false))
    }

    @Test fun `running and unlocked is usable`() {
        assertEquals(SpaceUsability.Usable, spaceUsability(true, true, true))
    }
}
