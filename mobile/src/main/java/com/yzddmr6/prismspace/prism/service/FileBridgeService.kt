package com.yzddmr6.prismspace.prism.service

import android.app.Activity
import android.app.admin.DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT
import android.content.ComponentName
import android.content.ContentValues
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import com.yzddmr6.prismspace.engine.CrossProfile
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.model.PerAppFileSharePolicy
import com.yzddmr6.prismspace.prism.model.PerAppFileShareSpec
import com.yzddmr6.prismspace.prism.model.PerAppShareDestination
import com.yzddmr6.prismspace.prism.ui.ProfileImagePickerActivity
import com.yzddmr6.prismspace.shuttle.Shuttle
import com.yzddmr6.prismspace.util.DPM
import com.yzddmr6.prismspace.util.DevicePolicies
import com.yzddmr6.prismspace.util.PrismLocale
import com.yzddmr6.prismspace.util.Users
import com.yzddmr6.prismspace.util.Users.Companion.toId
import java.nio.charset.StandardCharsets

data class FileBridgeSelfTestResult(
    val success: Boolean,
    val message: String,
    val cloneUri: String? = null,
    val mainUri: String? = null,
)

data class FileTransferResult(
    val success: Boolean,
    val message: String,
    val displayName: String? = null,
    val targetUri: String? = null,
)

data class PerAppShareFolderResult(
    val success: Boolean,
    val message: String,
    val relativePath: String? = null,
    val markerDisplayName: String? = null,
)

class FileBridgeService {

    /** Localized user-facing message (follows the app's chosen language, not the system default). */
    private fun str(context: Context, id: Int, vararg args: Any): String =
        PrismLocale.wrap(context).getString(id, *args)

    fun runSelfTest(context: Context): FileBridgeSelfTestResult {
        return try {
            DiagnosticLog.i(TAG, "self-test start package=${context.packageName}")
            val profile = Users.profile ?: return FileBridgeSelfTestResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            DiagnosticLog.i(TAG, "profile resolved id=${profile.toId()}")
            val payload = buildPayload(context)
            val cloneResult = Shuttle(context, to = profile).invokeNoThrows {
                DiagnosticLog.i(TAG, "profile write start")
                val cloneUri = writeDownload(
                    displayName = CLONE_FILE,
                    mimeType = MIME_TEXT,
                    bytes = payload,
                )
                DiagnosticLog.i(TAG, "profile write done uri=$cloneUri")
                val bytes = contentResolver.openInputStream(Uri.parse(cloneUri))?.use { it.readBytes() }
                    ?: return@invokeNoThrows null
                DiagnosticLog.i(TAG, "profile read done bytes=${bytes.size}")
                BridgePayload(bytes, cloneUri)
            } ?: return FileBridgeSelfTestResult(
                success = false,
                message = "文件桥未就绪，请确认双开空间正在运行",
            )
            DiagnosticLog.i(TAG, "shuttle returned bytes=${cloneResult.bytes.size} uri=${cloneResult.uri}")

            DiagnosticLog.i(TAG, "main write start")
            val mainUri = context.writeDownload(
                displayName = MAIN_FILE,
                mimeType = MIME_TEXT,
                bytes = cloneResult.bytes,
            )
            DiagnosticLog.i(TAG, "main write done uri=$mainUri")
            FileBridgeSelfTestResult(
                success = true,
                message = "文件桥自检通过：主空间 -> 双开空间 -> 主空间",
                cloneUri = cloneResult.uri,
                mainUri = mainUri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "self-test failed", e)
            FileBridgeSelfTestResult(
                success = false,
                message = e.message ?: e.javaClass.simpleName,
            )
        }
    }

    fun importToProfile(context: Context, sourceUri: Uri): FileTransferResult {
        return try {
            DiagnosticLog.i(TAG, "import start uri=$sourceUri")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val payload = readPayload(context, sourceUri)
            val profileUri = Shuttle(context, to = profile).invokeNoThrows(with = payload) {
                writeDownload(
                    displayName = it.displayName,
                    mimeType = it.mimeType,
                    bytes = it.bytes,
                )
            } ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
            )
            DiagnosticLog.i(TAG, "import done display=${payload.displayName} uri=$profileUri")
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_imported_to_dual, payload.displayName),
                displayName = payload.displayName,
                targetUri = profileUri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "import failed", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_import_file_failed),
            )
        }
    }

    /**
     * Export from the dual space to the main space by copying the user-selected system URI into
     * the main user's MediaStore Downloads/PrismSpace folder. The system grants the main process
     * read access for the picked URI, so this stays local and avoids Binder-size limits.
     */
    fun importToMain(context: Context, sourceUri: Uri): FileTransferResult {
        return try {
            val payload = readPayload(context, sourceUri)
            val uri = AndroidFileBridgeDownloadStore(context).insert(
                payload.displayName, payload.mimeType, payload.bytes,
                FileBridgeDownloadWriter.DEFAULT_RELATIVE_PATH,
            )
            FileTransferResult(true, str(context, R.string.fb_imported_to_main, payload.displayName), payload.displayName, uri)
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "import to main failed", e)
            FileTransferResult(false, e.message ?: str(context, R.string.fb_import_file_failed))
        }
    }

    /** Export an image from the dual space to the main user's Pictures/PrismSpace folder. */
    fun importImageToMainGallery(context: Context, sourceUri: Uri): FileTransferResult {
        return try {
            val payload = readSharedMediaPayload(context, sourceUri)
            val uri = AndroidFileBridgeMediaStore(context).insert(
                payload.displayName, payload.mimeType, payload.bytes,
                FileBridgeMediaWriter.DEFAULT_RELATIVE_PATH,
            )
            FileTransferResult(true, str(context, R.string.fb_imported_to_main_gallery, payload.displayName), payload.displayName, uri)
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "image import to main failed", e)
            FileTransferResult(false, e.message ?: str(context, R.string.fb_import_image_failed))
        }
    }

    /**
     * 普通模式克隆: transfer a COMPLETE app — base + ALL split APKs — into the dual space's
     * Download/PrismSpace/, recording a single incoming history entry as "label-package". Copying
     * the whole split set keeps split packages installable. Only the APK path strings cross the Shuttle (Serializable); the profile process reads
     * each /data/app file by path (world-readable, same absolute path across users) and streams it into
     * its own MediaStore — avoiding the Binder byte cap and non-serializable PFDs, so it supports big APKs.
     */
    fun importApksToProfile(context: Context, apkFiles: List<java.io.File>, label: String, packageName: String): FileTransferResult {
        return try {
            val profile = Users.profile ?: return FileTransferResult(false, str(context, R.string.fb_need_create_space))
            val paths = ArrayList(apkFiles.filter { it.canRead() }.map { it.absolutePath })
            if (paths.isEmpty()) return FileTransferResult(false, str(context, R.string.fb_apk_unreadable))
            val safeBase = FileTransferPolicy.safeDisplayName("$label-$packageName")
            val cloneLocation = "Download/PrismSpace"
            val firstUri = Shuttle(context, to = profile).invokeNoThrows(with = paths) { list ->
                var first: String? = null
                list.forEachIndexed { i, path ->
                    val name = if (i == 0) "$safeBase.apk" else "$safeBase.split$i.apk"
                    val uri = AndroidFileBridgeDownloadStore(this).insertFromFile(
                        name, "application/vnd.android.package-archive",
                        java.io.File(path), FileBridgeDownloadWriter.DEFAULT_RELATIVE_PATH)
                    if (first == null) first = uri
                }
                // Dual-space incoming half: recorded as label + package, shown as "label-package".
                TransferHistoryStore.record(this, label, cloneLocation, false, packageName)
                first
            } ?: return FileTransferResult(false, str(context, R.string.fb_space_not_ready))
            FileTransferResult(true, str(context, R.string.fb_apk_transferred, safeBase), safeBase, firstUri)
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "apks import failed", e)
            FileTransferResult(false, e.message ?: str(context, R.string.fb_apk_transfer_failed))
        }
    }

    fun importImageToProfileGallery(context: Context, sourceUri: Uri): FileTransferResult {
        return try {
            DiagnosticLog.i(TAG, "media sync start uri=$sourceUri")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val payload = readSharedMediaPayload(context, sourceUri)
            val profileUri = Shuttle(context, to = profile).invokeNoThrows(with = payload) {
                writeSharedImage(
                    displayName = it.displayName,
                    mimeType = it.mimeType,
                    bytes = it.bytes,
                )
            } ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
            )
            DiagnosticLog.i(TAG, "media sync done display=${payload.displayName} uri=$profileUri")
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_synced_dual_gallery, payload.displayName),
                displayName = payload.displayName,
                targetUri = profileUri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "media sync failed", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_sync_photo_failed),
            )
        }
    }

    fun importImageToPerAppShare(context: Context, sourceUri: Uri, packageName: String): FileTransferResult {
        return try {
            DiagnosticLog.i(TAG, "shared media import start sourcePackage=$packageName uri=$sourceUri")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val payload = PerAppFileBridgePayload(
                file = readSharedMediaPayload(context, sourceUri),
                relativePath = PerAppShareDestination.mediaRelativePath(packageName),
            )
            val profileUri = Shuttle(context, to = profile).invokeNoThrows(with = payload) { request ->
                FileBridgeMediaWriter(AndroidFileBridgeMediaStore(this)).write(
                    displayName = request.file.displayName,
                    mimeType = request.file.mimeType,
                    bytes = request.file.bytes,
                    relativePath = request.relativePath,
                )
            } ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
            )
            DiagnosticLog.i(TAG, "shared media import done sourcePackage=$packageName display=${payload.file.displayName} uri=$profileUri")
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_synced_shared_gallery, payload.file.displayName),
                displayName = payload.file.displayName,
                targetUri = profileUri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "shared media import failed sourcePackage=$packageName", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_sync_photo_failed),
            )
        }
    }

    fun importFileToPerAppShare(context: Context, sourceUri: Uri, packageName: String): FileTransferResult {
        return try {
            DiagnosticLog.i(TAG, "shared file import start sourcePackage=$packageName uri=$sourceUri")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val payload = PerAppFileBridgePayload(
                file = readPayload(context, sourceUri),
                relativePath = PerAppShareDestination.downloadRelativePath(packageName),
            )
            val profileUri = Shuttle(context, to = profile).invokeNoThrows(with = payload) { request ->
                FileBridgeDownloadWriter(AndroidFileBridgeDownloadStore(this)).write(
                    displayName = request.file.displayName,
                    mimeType = request.file.mimeType,
                    bytes = request.file.bytes,
                    relativePath = request.relativePath,
                )
            } ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
            )
            DiagnosticLog.i(TAG, "shared file import done sourcePackage=$packageName display=${payload.file.displayName} uri=$profileUri")
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_imported_shared_dir, payload.file.displayName),
                displayName = payload.file.displayName,
                targetUri = profileUri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "shared file import failed sourcePackage=$packageName", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_import_file_failed),
            )
        }
    }

    fun verifyProfileGalleryVisibility(context: Context): FileTransferResult {
        return try {
            DiagnosticLog.i(TAG, "media visibility start")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val entry = Shuttle(context, to = profile).invokeNoThrows {
                FileBridgeMediaVisibilityVerifier(AndroidFileBridgeMediaQueryStore(this)).latestVisibleImage()
            } ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_no_dual_media),
            )
            DiagnosticLog.i(TAG, "media visibility found display=${entry.displayName} uri=${entry.uri}")
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_dual_media_visible, entry.displayName),
                displayName = entry.displayName,
                targetUri = entry.uri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "media visibility failed", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_check_media_failed),
            )
        }
    }

    fun openProfileImagePicker(activity: Activity): FileTransferResult {
        val context = activity.applicationContext
        return try {
            DiagnosticLog.i(TAG, "profile image picker launch start")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val launched = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                openProfileImagePickerViaForwarder(activity, profile)
            } else {
                Shuttle(context, to = profile).launchNoThrows {
                    ProfileImagePickerLauncher.open(this)
                }
            }
            if (!launched) return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
            )
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_opened_image_picker),
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "profile image picker launch failed", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_open_image_picker_failed),
            )
        }
    }

    private fun openProfileImagePickerViaForwarder(activity: Activity, profile: UserHandle): Boolean {
        val context = activity.applicationContext
        val intent = prepareProfileImagePickerForwarderIntent(context, profile) ?: return false
        activity.startActivity(intent)
        return true
    }

    fun openProfileDownloadsFolder(activity: Activity): FileTransferResult =
        ProfileDownloadsOpener().openDownloadsFolder(activity)

    fun openProfileInstallEntry(activity: Activity): FileTransferResult =
        ProfileDownloadsOpener().openInstallEntry(activity)

    fun openProfileInstallSourceSettings(activity: Activity, packageName: String): FileTransferResult =
        ProfileDownloadsOpener().openInstallSourceSettings(activity, packageName)

    private fun prepareProfileImagePickerForwarderIntent(context: Context, profile: UserHandle): Intent? {
        val intent = ProfileImagePickerLauncher.buildCrossProfileActivityIntent()
        installProfileImagePickerForwarding(context, profile)
        val forwarder = findCrossProfileForwarder(context, intent)
            ?: return null
        return intent.setComponent(forwarder).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }

    private fun installProfileImagePickerForwarding(context: Context, profile: UserHandle): Boolean {
        return Shuttle(context, to = profile).invokeNoThrows {
            val filter = ProfileImagePickerLauncher.crossProfileActivityIntentFilter()
            val policies = DevicePolicies(this)
            policies.addCrossProfileIntentFilter(
                filter,
                ProfileImagePickerLauncher.crossProfileForwardingFlags(),
            )
            policies.execute(
                DPM::addPersistentPreferredActivity,
                filter,
                ProfileImagePickerLauncher.crossProfilePreferredActivityComponent(this),
            )
            true
        } == true
    }

    private fun findCrossProfileForwarder(context: Context, intent: Intent): ComponentName? {
        return context.packageManager.queryIntentActivities(
            Intent(intent).setComponent(null),
            MATCH_DISABLED_COMPONENTS or MATCH_DEFAULT_ONLY,
        )
            .firstOrNull { it.activityInfo.packageName == "android" }
            ?.activityInfo
            ?.run { ComponentName(packageName, name) }
    }

    fun exportLatestToMain(context: Context): FileTransferResult {
        return try {
            DiagnosticLog.i(TAG, "export latest start")
            val profile = Users.profile ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val payload = Shuttle(context, to = profile).invokeNoThrows {
                AndroidFileBridgeDownloadStore(this).readLatestInPrismFolder()
            } ?: return FileTransferResult(
                success = false,
                message = str(context, R.string.fb_no_files_to_export),
            )
            val targetUri = context.writeDownload(
                displayName = payload.displayName,
                mimeType = payload.mimeType,
                bytes = payload.bytes,
            )
            DiagnosticLog.i(TAG, "export latest done display=${payload.displayName} uri=$targetUri")
            FileTransferResult(
                success = true,
                message = str(context, R.string.fb_exported_to_main, payload.displayName),
                displayName = payload.displayName,
                targetUri = targetUri,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "export latest failed", e)
            FileTransferResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_export_failed),
            )
        }
    }

    fun enablePerAppShareFolder(context: Context, packageName: String): PerAppShareFolderResult {
        return try {
            DiagnosticLog.i(TAG, "per-app share enable start package=$packageName")
            val profile = Users.profile ?: return PerAppShareFolderResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val spec = PerAppFileSharePolicy.specFor(packageName)
            val markerUri = Shuttle(context, to = profile).invokeNoThrows(with = spec) {
                AndroidPerAppShareFolderStore(this).writeMarker(it)
            } ?: return PerAppShareFolderResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
                relativePath = spec.relativePath,
                markerDisplayName = spec.markerDisplayName,
            )
            DiagnosticLog.i(TAG, "per-app share enabled package=$packageName marker=$markerUri")
            PerAppShareFolderResult(
                success = true,
                message = str(context, R.string.fb_shared_dir_ready, spec.relativePath),
                relativePath = spec.relativePath,
                markerDisplayName = spec.markerDisplayName,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "per-app share enable failed package=$packageName", e)
            PerAppShareFolderResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_prepare_shared_dir_failed),
            )
        }
    }

    fun disablePerAppShareFolder(context: Context, packageName: String): PerAppShareFolderResult {
        return try {
            DiagnosticLog.i(TAG, "per-app share disable start package=$packageName")
            val profile = Users.profile ?: return PerAppShareFolderResult(
                success = false,
                message = str(context, R.string.fb_need_create_space),
            )
            val spec = PerAppFileSharePolicy.specFor(packageName)
            val deleted = Shuttle(context, to = profile).invokeNoThrows(with = spec) {
                AndroidPerAppShareFolderStore(this).deleteMarker(it)
                true
            } == true
            if (!deleted) return PerAppShareFolderResult(
                success = false,
                message = str(context, R.string.fb_space_not_ready),
                relativePath = spec.relativePath,
                markerDisplayName = spec.markerDisplayName,
            )
            DiagnosticLog.i(TAG, "per-app share disabled package=$packageName")
            PerAppShareFolderResult(
                success = true,
                message = str(context, R.string.fb_isolation_restored),
                relativePath = spec.relativePath,
                markerDisplayName = spec.markerDisplayName,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "per-app share disable failed package=$packageName", e)
            PerAppShareFolderResult(
                success = false,
                message = e.message ?: str(context, R.string.fb_restore_isolation_failed),
            )
        }
    }

    private fun readPayload(context: Context, sourceUri: Uri): FileBridgePayload {
        val resolver = context.contentResolver
        val displayName = FileTransferPolicy.safeDisplayName(queryDisplayName(resolver, sourceUri))
        // No size cap: large files stream through without an upper limit.
        val bytes = resolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: error(str(context, R.string.fb_read_selected_failed))
        val mimeType = resolver.getType(sourceUri) ?: "application/octet-stream"
        return FileBridgePayload(displayName, mimeType, bytes)
    }

    private fun readSharedMediaPayload(context: Context, sourceUri: Uri): FileBridgePayload {
        val resolver = context.contentResolver
        val displayName = FileTransferPolicy.safeDisplayName(queryDisplayName(resolver, sourceUri))
        val rawMimeType = resolver.getType(sourceUri)
        val mimeType = FileTransferPolicy.resolveSharedMediaMimeType(rawMimeType, displayName)
            ?: error(str(context, R.string.fb_select_image))
        if (!FileTransferPolicy.isSupportedSharedMediaMimeType(mimeType, displayName)) error(str(context, R.string.fb_select_image))
        val bytes = resolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: error(str(context, R.string.fb_read_selected_image_failed))
        return FileBridgePayload(displayName, mimeType, bytes)
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }

    private fun querySize(resolver: ContentResolver, uri: Uri): Long? =
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
        }

    private fun buildPayload(context: Context): ByteArray {
        val text = buildString {
            appendLine("PrismSpace file bridge self-test")
            appendLine("package=${context.packageName}")
            appendLine("timestamp=${System.currentTimeMillis()}")
        }
        return text.toByteArray(StandardCharsets.UTF_8)
    }

    private data class BridgePayload(
        val bytes: ByteArray,
        val uri: String,
    ) : java.io.Serializable

    private companion object {
        private const val TAG = "Prism.FileBridge"
        private const val MIME_TEXT = "text/plain"
        private const val MIME_JSON = "application/json"
        private const val CLONE_FILE = "prismspace-bridge-clone.txt"
        private const val MAIN_FILE = "prismspace-bridge-main.txt"
    }
}

private data class FileBridgePayload(
    val displayName: String,
    val mimeType: String,
    val bytes: ByteArray,
) : java.io.Serializable

private data class PerAppFileBridgePayload(
    val file: FileBridgePayload,
    val relativePath: String,
) : java.io.Serializable

data class ProfileMediaEntry(
    val displayName: String,
    val mimeType: String,
    val uri: String,
) : java.io.Serializable

private fun Context.writeDownload(displayName: String, mimeType: String, bytes: ByteArray): String {
    return FileBridgeDownloadWriter(AndroidFileBridgeDownloadStore(this)).write(displayName, mimeType, bytes)
}

private fun Context.writeSharedImage(displayName: String, mimeType: String, bytes: ByteArray): String {
    return FileBridgeMediaWriter(AndroidFileBridgeMediaStore(this)).write(displayName, mimeType, bytes)
}

internal class FileBridgeDownloadWriter(private val store: FileBridgeDownloadStore) {

    fun write(
        displayName: String,
        mimeType: String,
        bytes: ByteArray,
        relativePath: String = DEFAULT_RELATIVE_PATH,
    ): String {
        // Do NOT delete an existing same-name file — that silently destroyed the user's data.
        // MediaStore auto-appends " (1)" on a DISPLAY_NAME collision in the same RELATIVE_PATH (Q+),
        // so a second "report.pdf" becomes "report (1).pdf" and the original is preserved.
        return store.insert(displayName, mimeType, bytes, relativePath)
    }

    companion object {
        const val DEFAULT_RELATIVE_PATH = "Download/PrismSpace/"
    }
}

internal interface FileBridgeDownloadStore {
    fun deleteByDisplayName(displayName: String)
    fun insert(displayName: String, mimeType: String, bytes: ByteArray, relativePath: String): String
}

internal class FileBridgeMediaWriter(private val store: FileBridgeMediaStore) {

    fun write(
        displayName: String,
        mimeType: String,
        bytes: ByteArray,
        relativePath: String = DEFAULT_RELATIVE_PATH,
    ): String {
        // MediaStore auto-dedups the on-disk name on collision.
        return store.insert(displayName, mimeType, bytes, relativePath)
    }

    companion object {
        const val DEFAULT_RELATIVE_PATH = "Pictures/PrismSpace/"
    }
}

internal interface FileBridgeMediaStore {
    fun deleteByDisplayName(displayName: String)
    fun insert(displayName: String, mimeType: String, bytes: ByteArray, relativePath: String): String
}

internal class FileBridgeMediaVisibilityVerifier(private val store: FileBridgeMediaQueryStore) {
    fun latestVisibleImage(): ProfileMediaEntry? = store.readLatestInPrismPictures()
}

internal interface FileBridgeMediaQueryStore {
    fun readLatestInPrismPictures(): ProfileMediaEntry?
}

internal object ProfileImagePickerLauncher {
    private const val ACTION_PROFILE_IMAGE_PICKER = "com.yzddmr6.prismspace.action.PROFILE_IMAGE_PICKER"

    fun intentSpec() = ProfileImagePickerIntentSpec(
        action = Intent.ACTION_GET_CONTENT,
        type = "image/*",
        categories = setOf(Intent.CATEGORY_OPENABLE),
        flags = Intent.FLAG_ACTIVITY_NEW_TASK,
    )

    fun crossProfileActivityIntentSpec() = ProfileImagePickerActivityIntentSpec(
        action = ACTION_PROFILE_IMAGE_PICKER,
        categories = setOf(CrossProfile.CATEGORY_MANAGED_PROFILE, Intent.CATEGORY_DEFAULT),
    )

    fun buildIntent(): Intent {
        val spec = intentSpec()
        return Intent(spec.action)
            .setType(spec.type)
            .addFlags(spec.flags)
            .also { intent -> spec.categories.forEach(intent::addCategory) }
    }

    fun buildCrossProfileActivityIntent(): Intent {
        val spec = crossProfileActivityIntentSpec()
        return Intent(spec.action)
            .also { intent -> spec.categories.forEach(intent::addCategory) }
    }

    fun crossProfileActivityIntentFilter(): IntentFilter {
        val spec = crossProfileActivityIntentSpec()
        return IntentFilter(spec.action)
            .also { filter -> spec.categories.forEach(filter::addCategory) }
    }

    fun crossProfileForwardingFlags() = FLAG_MANAGED_CAN_ACCESS_PARENT

    fun crossProfilePreferredActivityClassName() = ProfileImagePickerActivity::class.java.name

    fun crossProfilePreferredActivityComponent(context: Context) = ComponentName(
        context.packageName,
        crossProfilePreferredActivityClassName(),
    )

    fun open(context: Context) {
        context.startActivity(buildIntent())
    }
}

internal data class ProfileImagePickerIntentSpec(
    val action: String,
    val type: String,
    val categories: Set<String>,
    val flags: Int,
)

internal data class ProfileImagePickerActivityIntentSpec(
    val action: String,
    val categories: Set<String>,
)

internal object FileTransferPolicy {
    // No fixed upper size limit: file bridge must handle large payloads such as APKs.
    // Large transfers stream via ParcelFileDescriptor to avoid Binder transaction size limits.
    fun isAllowedSize(size: Long) = size >= 0

    fun safeDisplayName(name: String?): String {
        val normalized = name
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            .orEmpty()
        return normalized.ifBlank { "prismspace-import.bin" }
    }

    fun isSupportedSharedMediaMimeType(mimeType: String?, displayName: String): Boolean =
        resolveSharedMediaMimeType(mimeType, displayName)?.startsWith("image/") == true

    fun resolveSharedMediaMimeType(mimeType: String?, displayName: String): String? {
        val normalized = mimeType?.lowercase()?.takeUnless { it == "application/octet-stream" }
        if (normalized?.startsWith("image/") == true) return normalized
        return when (displayName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> normalized
        }
    }
}

private class AndroidFileBridgeDownloadStore(private val context: Context) : FileBridgeDownloadStore {

    override fun deleteByDisplayName(displayName: String) {
        context.contentResolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
        )
    }

    override fun insert(displayName: String, mimeType: String, bytes: ByteArray, relativePath: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create Downloads entry")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("Unable to open Downloads output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
        return uri.toString()
    }

    /** Stream from a source File (read locally inside this — the profile — process) into a MediaStore
     *  Downloads entry. No full-memory load, no Binder byte limit. Used by the file-sync clone path to copy a
     *  shared, world-readable /data/app APK into the dual space for manual install. */
    fun insertFromFile(displayName: String, mimeType: String, src: java.io.File, relativePath: String): String {
        // No pre-delete: cloning the same app twice now yields "App (1).apk" instead of destroying the first.
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create Downloads entry")
        src.inputStream().use { input ->
            resolver.openOutputStream(uri)?.use { output -> input.copyTo(output, 64 * 1024) }
                ?: error("Unable to open Downloads output stream")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
        return uri.toString()
    }

    fun readLatestInPrismFolder(): FileBridgePayload? {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        } else {
            null
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(PRISM_RELATIVE_PATH)
        } else {
            null
        }
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val displayName = FileTransferPolicy.safeDisplayName(
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                )
                val mimeTypeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val mimeType = if (mimeTypeIndex >= 0 && !cursor.isNull(mimeTypeIndex)) {
                    cursor.getString(mimeTypeIndex)
                } else {
                    "application/octet-stream"
                }
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
                return FileBridgePayload(displayName, mimeType, bytes)
            }
        }
        return null
    }

    private companion object {
        private const val PRISM_RELATIVE_PATH = "Download/PrismSpace/"
    }
}

private class AndroidPerAppShareFolderStore(private val context: Context) {

    fun writeMarker(spec: PerAppFileShareSpec): String {
        deleteMarker(spec)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, spec.markerDisplayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, spec.relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create per-app share marker")
        resolver.openOutputStream(uri)?.use { it.write(spec.markerBytes) }
            ?: error("Unable to write per-app share marker")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
        return uri.toString()
    }

    fun deleteMarker(spec: PerAppFileShareSpec) {
        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        } else {
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(spec.markerDisplayName, spec.relativePath)
        } else {
            arrayOf(spec.markerDisplayName)
        }
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                resolver.delete(ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id), null, null)
            }
        }
    }
}

private class AndroidFileBridgeMediaStore(private val context: Context) : FileBridgeMediaStore {

    override fun deleteByDisplayName(displayName: String) {
        context.contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
        )
    }

    override fun insert(displayName: String, mimeType: String, bytes: ByteArray, relativePath: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create Images entry")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("Unable to open Images output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
        return uri.toString()
    }
}

private class AndroidFileBridgeMediaQueryStore(private val context: Context) : FileBridgeMediaQueryStore {

    override fun readLatestInPrismPictures(): ProfileMediaEntry? {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        } else {
            null
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(PRISM_PICTURES_RELATIVE_PATH)
        } else {
            null
        }
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val displayName = FileTransferPolicy.safeDisplayName(
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                )
                val mimeTypeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val mimeType = if (mimeTypeIndex >= 0 && !cursor.isNull(mimeTypeIndex)) {
                    cursor.getString(mimeTypeIndex)
                } else {
                    "image/*"
                }
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                return ProfileMediaEntry(displayName, mimeType, uri.toString())
            }
        }
        return null
    }

    private companion object {
        private const val PRISM_PICTURES_RELATIVE_PATH = "Pictures/PrismSpace/"
    }
}
