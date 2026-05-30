package com.yzddmr6.prismspace.prism.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileBridgeMediaVisibilityVerifierTest {

    @Test fun returnsLatestVisiblePrismPicture() {
        val verifier = FileBridgeMediaVisibilityVerifier(
            RecordingMediaQueryStore(ProfileMediaEntry("photo.png", "image/png", "content://images/1"))
        )

        val entry = verifier.latestVisibleImage()

        assertEquals("photo.png", entry?.displayName)
        assertEquals("image/png", entry?.mimeType)
        assertEquals("content://images/1", entry?.uri)
    }

    @Test fun returnsNullWhenNoPrismPictureExists() {
        val verifier = FileBridgeMediaVisibilityVerifier(RecordingMediaQueryStore(null))

        assertNull(verifier.latestVisibleImage())
    }

    private class RecordingMediaQueryStore(
        private val result: ProfileMediaEntry?,
    ) : FileBridgeMediaQueryStore {
        override fun readLatestInPrismPictures(): ProfileMediaEntry? = result
    }
}
