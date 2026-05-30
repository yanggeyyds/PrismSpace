package com.yzddmr6.prismspace.prism.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import android.widget.Toast
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.vm.isImageMime
import com.yzddmr6.prismspace.prism.service.TransferHistoryStore
import com.yzddmr6.prismspace.util.PrismLocale
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Unified file-transfer receiver (bidirectional, normal permissions). PrismSpace is installed in
 * both the main and the dual space, so this single ACTION_SEND target appears in the system share
 * chooser's 个人/工作 tabs as "导入到此空间 PrismSpace". Whichever space's copy receives the share
 * writes the file into THAT space — selecting the 个人 tab imports to the main space, the 工作 tab
 * imports to the dual space. Same name, both directions correct.
 *
 * Why share (not SAF): some ROMs intercept the document picker across profile boundaries, while
 * the system share chooser keeps the Personal/Work routing explicit.
 *
 * Flow: receive shared file → copy it to a private cache temp (reads the cross-profile-granted URI
 * immediately, before it can expire) → let the user choose the exact destination file via the
 * system save panel (CREATE_DOCUMENT) → write the file there → record in the persisted transfer
 * history.
 */
class ImportToSpaceActivity : Activity() {

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(PrismLocale.wrap(newBase))

    private var temp: File? = null
    private var displayName: String = "file"
    private var mime: String = "application/octet-stream"
    private var isImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val uri: Uri? = @Suppress("DEPRECATION")
                (intent?.getParcelableExtra(Intent.EXTRA_STREAM)
                    ?: intent?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri)
            if (uri == null) {
                toast(getString(R.string.lz_io_no_file))
                finish(); return
            }
            mime = intent?.type ?: contentResolver.getType(uri) ?: "application/octet-stream"
            isImage = isImageMime(mime)
            displayName = queryName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            DiagnosticLog.i(TAG, "import receive start name=$displayName mime=$mime uri=$uri")
            // Read the (possibly cross-profile-granted) URI NOW into a private temp, so the folder
            // picker round-trip can't lose the grant.
            temp = copyToCache(uri)
            if (temp == null) {
                toast(getString(R.string.lz_io_cant_read))
                finish(); return
            }
            // Let the user choose where to save this one file. CREATE_DOCUMENT avoids Android 11+
            // tree-grant restrictions that gray out root/Download folders in OPEN_DOCUMENT_TREE.
            startActivityForResult(
                ImportDestinationPlanner.buildCreateDocumentIntent(displayName, mime),
                REQ_CREATE_DOCUMENT,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "import receive failed", e)
            toast(getString(R.string.lz_io_failed, e.message ?: e.javaClass.simpleName))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CREATE_DOCUMENT) { finish(); return }
        val target = data?.data
        val src = temp
        try {
            if (resultCode != RESULT_OK || target == null || src == null) {
                toast(getString(R.string.lz_io_cancelled))
                return
            }
            writeToDocument(target, src)
            val location = ImportDestinationPlanner.displayLocationForCreatedDocument(target.toString())
            TransferHistoryStore.record(this, displayName, location, isImage)
            DiagnosticLog.i(TAG, "import write done name=$displayName location=$location target=$target")
            toast(getString(R.string.lz_io_done, displayName))
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "import write failed", e)
            toast(getString(R.string.lz_io_failed, e.message ?: e.javaClass.simpleName))
        } finally {
            temp?.delete()
            finish()
        }
    }

    /** Copy [src] into a private cache temp; returns the temp file (or null on failure). */
    private fun copyToCache(src: Uri): File? = runCatching {
        val dir = File(cacheDir, "import").apply { mkdirs() }
        val out = File(dir, "in_${System.currentTimeMillis()}")
        contentResolver.openInputStream(src)!!.use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        out
    }.getOrNull()

    /** Stream [src] into the user-created document. */
    private fun writeToDocument(targetUri: Uri, src: File) {
        contentResolver.openOutputStream(targetUri).use { output ->
            src.inputStream().use { it.copyTo(output!!) }
        }
    }

    private fun queryName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) c.getString(idx) else null
            } else null
        }
    }.getOrNull()

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private companion object {
        private const val TAG = "Prism.ImportToSpace"
        private const val REQ_CREATE_DOCUMENT = 4201
    }
}

internal object ImportDestinationPlanner {
    fun createDocumentIntentSpec(displayName: String, mimeType: String): CreateDocumentIntentSpec =
        CreateDocumentIntentSpec(
            action = Intent.ACTION_CREATE_DOCUMENT,
            type = mimeType,
            title = displayName,
            categories = setOf(Intent.CATEGORY_OPENABLE),
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )

    fun buildCreateDocumentIntent(displayName: String, mimeType: String): Intent {
        val spec = createDocumentIntentSpec(displayName, mimeType)
        return Intent(spec.action)
            .setType(spec.type)
            .putExtra(Intent.EXTRA_TITLE, spec.title)
            .addFlags(spec.flags)
            .also { intent -> spec.categories.forEach(intent::addCategory) }
    }

    fun displayLocationForCreatedDocument(uriString: String): String {
        documentIdFrom(uriString)?.let { docId ->
            val path = docId.substringAfter(':', docId)
            val parent = path.substringBeforeLast('/', "")
            if (parent.isNotBlank()) return parent
        }
        return authorityFrom(uriString)
    }

    private fun documentIdFrom(uriString: String): String? {
        val encodedDocumentId = uriString
            .substringAfter("/document/", missingDelimiterValue = "")
            .substringBefore('?')
            .substringBefore('#')
        return URLDecoder.decode(encodedDocumentId, StandardCharsets.UTF_8.name()).takeIf { it.isNotBlank() }
    }

    private fun authorityFrom(uriString: String): String =
        uriString
            .substringAfter("://", missingDelimiterValue = "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
}

internal data class CreateDocumentIntentSpec(
    val action: String,
    val type: String,
    val title: String,
    val categories: Set<String>,
    val flags: Int,
)
