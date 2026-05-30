package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.isImageMime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests pure helpers used by FilesViewModel.
 */
class FilesXferTest {

    // MIME classification

    @Test
    fun `isImageMime - image-jpeg returns true`() {
        assertTrue(isImageMime("image/jpeg"))
    }

    @Test
    fun `isImageMime - image-png returns true`() {
        assertTrue(isImageMime("image/png"))
    }

    @Test
    fun `isImageMime - image-webp returns true`() {
        assertTrue(isImageMime("image/webp"))
    }

    @Test
    fun `isImageMime - application-pdf returns false`() {
        assertFalse(isImageMime("application/pdf"))
    }

    @Test
    fun `isImageMime - application-octet-stream returns false`() {
        assertFalse(isImageMime("application/octet-stream"))
    }

    @Test
    fun `isImageMime - null returns false`() {
        assertFalse(isImageMime(null))
    }

    @Test
    fun `isImageMime - IMAGE-JPEG uppercase returns true (case-insensitive)`() {
        assertTrue(isImageMime("IMAGE/JPEG"))
    }
}
