package com.yzddmr6.prismspace.prism.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerAppFileSharePolicyTest {

    @Test fun buildsSharedDownloadRelativePath() {
        val spec = PerAppFileSharePolicy.specFor("mark.via")

        assertEquals("mark.via", spec.safePackageSegment)
        assertEquals("Download/PrismSpace/", spec.relativePath)
        assertEquals("prismspace-share-policy-mark.via.json", spec.markerDisplayName)
    }

    @Test fun sanitizesPackageSegmentForMarkerName() {
        val spec = PerAppFileSharePolicy.specFor("../bad/pkg")

        assertEquals(".._bad_pkg", spec.safePackageSegment)
        assertFalse(spec.relativePath.contains("../bad/pkg"))
        assertEquals("Download/PrismSpace/", spec.relativePath)
        assertEquals("prismspace-share-policy-.._bad_pkg.json", spec.markerDisplayName)
    }

    @Test fun markerPayloadIncludesPackageAndRelativePath() {
        val payload = PerAppFileSharePolicy.specFor("mark.via").markerPayload

        assertTrue(payload.contains("\"packageName\":\"mark.via\""))
        assertTrue(payload.contains("\"relativePath\":\"Download/PrismSpace/\""))
        assertTrue(payload.contains("\"mode\":\"ImportExportOnly\""))
    }
}
