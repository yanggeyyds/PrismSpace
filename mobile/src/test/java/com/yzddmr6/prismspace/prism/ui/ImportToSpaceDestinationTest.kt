package com.yzddmr6.prismspace.prism.ui

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportToSpaceDestinationTest {

    @Test fun destinationPickerCreatesOneDocumentInsteadOfRequestingFolderTree() {
        val spec = ImportDestinationPlanner.createDocumentIntentSpec("report.pdf", "application/pdf")

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, spec.action)
        assertEquals("application/pdf", spec.type)
        assertEquals("report.pdf", spec.title)
        assertTrue(spec.categories.contains(Intent.CATEGORY_OPENABLE))
    }

    @Test fun displayLocationUsesCreatedDocumentParentWhenPossible() {
        val location = ImportDestinationPlanner.displayLocationForCreatedDocument(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FWork%2Freport.pdf",
        )

        assertEquals("Documents/Work", location)
    }

    @Test fun displayLocationFallsBackToAuthorityForOpaqueProviderUris() {
        val location = ImportDestinationPlanner.displayLocationForCreatedDocument(
            "content://com.example.provider/item/42",
        )

        assertEquals("com.example.provider", location)
    }
}
