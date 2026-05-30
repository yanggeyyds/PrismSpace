package com.yzddmr6.prismspace.prism.compose.vm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #8b: locks the in-app update version comparison (GitHub tag_name vs installed versionName). */
class CompareSemverTest {

    @Test fun newerPatchIsGreater() {
        assertTrue(compareSemver("0.0.2", "0.0.1") > 0)
    }

    @Test fun equalIsZero() {
        assertEquals(0, compareSemver("1.0.0", "1.0.0"))
    }

    @Test fun olderIsNegative() {
        assertTrue(compareSemver("0.0.1", "0.1.0") < 0)
        assertTrue(compareSemver("0.9.9", "1.0.0") < 0)
    }

    @Test fun vPrefixStripped() {
        assertTrue(compareSemver("v1.2.0", "1.1.9") > 0)
        assertEquals(0, compareSemver("v0.0.1", "0.0.1"))
    }

    @Test fun differentSegmentCounts() {
        assertEquals(0, compareSemver("1.0", "1.0.0"))
        assertTrue(compareSemver("1.0.1", "1.0") > 0)
    }

    @Test fun nonNumericSuffixIgnoredPerSegment() {
        // "3-beta" → leading digits "3"; same numeric value as "3".
        assertEquals(0, compareSemver("1.2.3-beta", "1.2.3"))
    }
}
