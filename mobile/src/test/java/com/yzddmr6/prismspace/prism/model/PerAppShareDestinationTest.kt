package com.yzddmr6.prismspace.prism.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PerAppShareDestinationTest {

    @Test fun mediaPathUsesSharedProfileFolder() {
        assertEquals("Pictures/PrismSpace/", PerAppShareDestination.mediaRelativePath("mark.via"))
    }

    @Test fun downloadPathUsesSharedProfileFolder() {
        assertEquals("Download/PrismSpace/", PerAppShareDestination.downloadRelativePath("mark.via"))
    }

    @Test fun unsafePackageCharactersAreReplaced() {
        assertEquals("bad_pkg.name_", PerAppShareDestination.safePackageSegment("bad/pkg.name!"))
    }
}
