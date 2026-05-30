package com.yzddmr6.prismspace.prism.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileTransferPolicyTest {

    @Test fun acceptsLargeFileSizeWithoutUpperLimit() {
        // No upper size limit (2026-05-23): even multi-hundred-MB APKs are allowed.
        assertTrue(FileTransferPolicy.isAllowedSize(500L * 1024L * 1024L))
    }

    @Test fun rejectsNegativeSize() {
        assertFalse(FileTransferPolicy.isAllowedSize(-1))
    }

    @Test fun normalizesBlankDisplayName() {
        assertEquals("prismspace-import.bin", FileTransferPolicy.safeDisplayName(""))
    }

    @Test fun removesPathSeparatorsFromDisplayName() {
        assertEquals("secret.txt", FileTransferPolicy.safeDisplayName("../secret.txt"))
    }

    @Test fun acceptsImageMimeForSharedMedia() {
        assertTrue(FileTransferPolicy.isSupportedSharedMediaMimeType("image/png", "photo.png"))
    }

    @Test fun rejectsNonImageMimeForSharedMedia() {
        assertFalse(FileTransferPolicy.isSupportedSharedMediaMimeType("text/plain", "note.txt"))
    }

    @Test fun infersJpegMimeForSharedMediaWhenProviderReturnsOctetStream() {
        assertEquals("image/jpeg", FileTransferPolicy.resolveSharedMediaMimeType("application/octet-stream", "photo.jpg"))
    }
}
