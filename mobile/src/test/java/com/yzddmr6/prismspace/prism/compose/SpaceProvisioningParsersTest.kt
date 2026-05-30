package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.space.SpaceCapProbe
import com.yzddmr6.prismspace.prism.compose.space.computeCap
import com.yzddmr6.prismspace.prism.compose.space.isRootOutput
import com.yzddmr6.prismspace.prism.compose.space.parsePmCreateOutput
import com.yzddmr6.prismspace.prism.compose.space.parsePmRemoveOutput
import com.yzddmr6.prismspace.prism.compose.space.PmCreateOutcome
import com.yzddmr6.prismspace.prism.compose.space.PmRemoveOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceProvisioningParsersTest {
    @Test fun `isRootOutput - null or empty is no root`() {
        assertFalse(isRootOutput(null)); assertFalse(isRootOutput(emptyList()))
        assertFalse(isRootOutput(listOf("", "   ")))
        assertTrue(isRootOutput(listOf("uid=0(root) gid=0(root)")))
    }
    @Test fun `parsePmCreateOutput - success extracts new user id`() {
        assertEquals(PmCreateOutcome.Created(12),
            parsePmCreateOutput(listOf("Success: created user id 12", "END")))
    }
    @Test fun `parsePmCreateOutput - user-limit reached`() {
        assertEquals(PmCreateOutcome.LimitReached,
            parsePmCreateOutput(listOf("Error: couldn't create User.", "Maximum number of users reached.")))
        assertEquals(PmCreateOutcome.LimitReached,
            parsePmCreateOutput(listOf("Error: couldn't create user limit reached")))
    }
    @Test fun `parsePmCreateOutput - generic failure keeps the line`() {
        assertEquals(PmCreateOutcome.Failed("Error: some other failure"),
            parsePmCreateOutput(listOf("Error: some other failure")))
    }
    @Test fun `parsePmCreateOutput - null-empty is failure`() {
        assertEquals(PmCreateOutcome.Failed(null), parsePmCreateOutput(null))
        assertEquals(PmCreateOutcome.Failed(null), parsePmCreateOutput(emptyList()))
    }
    @Test fun `parsePmRemoveOutput - success and failure`() {
        assertEquals(PmRemoveOutcome.Removed, parsePmRemoveOutput(listOf("Success: removed user")))
        assertEquals(PmRemoveOutcome.Failed("Error: couldn't remove user id 12"),
            parsePmRemoveOutput(listOf("Error: couldn't remove user id 12")))
        assertEquals(PmRemoveOutcome.Failed(null), parsePmRemoveOutput(null))
    }
    @Test fun `parsePmRemoveOutput - unsuccessful is not a false success`() {
        assertEquals(PmRemoveOutcome.Failed("Operation unsuccessful"),
            parsePmRemoveOutput(listOf("Operation unsuccessful")))
    }
    @Test fun `computeCap - known computes headroom, unknown when null max`() {
        assertEquals(SpaceCapProbe.Known(max = 4, current = 2), computeCap(maxUsers = 4, currentManaged = 1))
        assertEquals(SpaceCapProbe.Unknown, computeCap(maxUsers = null, currentManaged = 1))
        assertEquals(SpaceCapProbe.Known(max = 4, current = 1), computeCap(maxUsers = 4, currentManaged = 0))
    }
    @Test fun `parsePmCreateOutput - managed-profile limit classified`() {
        assertEquals(PmCreateOutcome.ManagedProfileLimit,
            parsePmCreateOutput(listOf(
                "Error: android.os.ServiceSpecificException: Cannot add more profiles of type android.os.usertype.profile.MANAGED for user 0 (code 6)",
                "Error: couldn't create User.", "END")))
    }
    @Test fun `parsePmCreateOutput - bare END sentinel never surfaced`() {
        assertEquals(PmCreateOutcome.Failed(null), parsePmCreateOutput(listOf("END")))
        assertEquals(PmCreateOutcome.Failed("Error: boom"),
            parsePmCreateOutput(listOf("Error: boom", "END")))
    }
}
