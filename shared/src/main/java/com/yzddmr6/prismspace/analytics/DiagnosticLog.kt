package com.yzddmr6.prismspace.analytics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.core.content.FileProvider
import com.yzddmr6.prismspace.util.Users
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class DiagnosticSection(
    val title: String,
    val body: String,
)

/**
 * Low-overhead local diagnostic log.
 *
 * Runtime logging is async and bounded to 2 MiB per user/process data dir. Export builds a text file
 * instead of using Intent.EXTRA_TEXT, because a useful diagnostic payload is larger than Binder's
 * practical extra-size limit.
 */
object DiagnosticLog {
    const val MAX_ROLLING_BYTES: Int = 2 * 1024 * 1024

    internal val TRIM_MARKER: ByteArray =
        "\n--- PrismSpace diagnostic log truncated; newest entries retained within 2 MiB budget ---\n".toByteArray(Charsets.UTF_8)

    private const val DIR = "diagnostics"
    private const val ROLLING_FILE = "prismspace-rolling.log"
    private const val EXPORT_PREFIX = "prismspace-diagnostics-"
    private const val MAX_EXPORT_FILES = 2
    private const val TRIM_HEADROOM_BYTES = 128 * 1024
    private const val LOGCAT_MAX_BYTES = 1 * 1024 * 1024
    private const val LOGCAT_TIMEOUT_MS = 1_500L
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "PrismDiagnosticLog").apply { isDaemon = true }
    }

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        record("I", "Prism.Diag", "initialized package=${context.packageName} user=${currentUserId()}")
    }

    fun v(tag: String, message: String) { Log.v(tag, message); record("V", tag, message) }
    fun d(tag: String, message: String) { Log.d(tag, message); record("D", tag, message) }
    fun i(tag: String, message: String) { Log.i(tag, message); record("I", tag, message) }
    fun w(tag: String, message: String, t: Throwable? = null) {
        if (t != null) Log.w(tag, message, t) else Log.w(tag, message)
        record("W", tag, message, t)
    }
    fun e(tag: String, message: String, t: Throwable? = null) {
        if (t != null) Log.e(tag, message, t) else Log.e(tag, message)
        record("E", tag, message, t)
    }

    fun record(level: String, tag: String, message: String, t: Throwable? = null) {
        val context = appContext ?: return
        val line = buildString {
            append(timestamp())
            append(' ')
            append(level.take(1))
            append('/')
            append(tag)
            append(" u=")
            append(currentUserId())
            append(" pid=")
            append(Process.myPid())
            append(": ")
            append(message.replace('\n', ' '))
            if (t != null) {
                append('\n')
                append(Log.getStackTraceString(t))
            }
            append('\n')
        }
        executor.execute {
            runCatching {
                val file = rollingFile(context)
                file.parentFile?.mkdirs()
                file.appendText(line, Charsets.UTF_8)
                if (file.length() > MAX_ROLLING_BYTES) {
                    file.writeBytes(trimToWindowBytes(file.readBytes(), MAX_ROLLING_BYTES - TRIM_HEADROOM_BYTES))
                }
            }.onFailure { Log.w("Prism.Diag", "failed to append diagnostic log", it) }
        }
    }

    fun createExportFile(
        context: Context,
        extraSections: List<DiagnosticSection> = emptyList(),
        includeLogcat: Boolean = true,
    ): File {
        val appContext = context.applicationContext
        flush()
        val dir = exportDir(appContext).apply { mkdirs() }
        cleanupOldExports(dir)
        val file = File(dir, "$EXPORT_PREFIX${fileTimestamp()}.txt")
        file.parentFile?.mkdirs()
        file.writeText(buildSnapshotText(appContext, extraSections, includeLogcat), Charsets.UTF_8)
        return file
    }

    fun openSnapshotDescriptor(context: Context): ParcelFileDescriptor {
        val file = createExportFile(context, includeLogcat = true)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", file)

    fun buildSnapshotText(
        context: Context,
        extraSections: List<DiagnosticSection> = emptyList(),
        includeLogcat: Boolean = true,
    ): String {
        flush()
        return buildString {
            appendLine("PrismSpace diagnostic report")
            appendLine("Generated: ${timestamp()}")
            appendLine("Package: ${context.packageName}")
            appendLine("Version: ${versionName(context)}")
            appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("User: ${currentUserId()}")
            appendLine("Process: pid=${Process.myPid()} uid=${Process.myUid()}")
            appendLine("Rolling log budget: ${MAX_ROLLING_BYTES} bytes per user")
            appendLine()

            extraSections.forEach { section ->
                appendSection(section.title, section.body)
            }

            appendSection("Rolling app log for current user", readRollingLog(context))
            if (includeLogcat) appendSection("Current logcat snapshot", collectLogcatSnapshot())
        }
    }

    internal fun trimToWindowBytes(input: ByteArray, maxBytes: Int): ByteArray {
        if (input.size <= maxBytes) return input
        if (maxBytes <= TRIM_MARKER.size) return input.copyOfRange(input.size - maxBytes, input.size)
        val tailSize = maxBytes - TRIM_MARKER.size
        val tail = input.copyOfRange(input.size - tailSize, input.size)
        return TRIM_MARKER + tail
    }

    private fun StringBuilder.appendSection(title: String, body: String) {
        appendLine("===== $title =====")
        if (body.isBlank()) appendLine("(empty)") else appendLine(body.trimEnd())
        appendLine()
    }

    private fun readRollingLog(context: Context): String =
        runCatching {
            val file = rollingFile(context)
            if (!file.exists()) "" else file.readText(Charsets.UTF_8)
        }.getOrElse { "Failed to read rolling log: ${it.message ?: it.javaClass.simpleName}" }

    private fun collectLogcatSnapshot(): String {
        val out = StringBuilder()
        var bytes = 0
        return runCatching {
            val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", "4000")
                .redirectErrorStream(true)
                .start()
            val reader = Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (shouldKeepLogcatLine(line) && bytes < LOGCAT_MAX_BYTES) {
                            val next = "$line\n"
                            val nextBytes = next.toByteArray(Charsets.UTF_8).size
                            if (bytes + nextBytes <= LOGCAT_MAX_BYTES) {
                                out.append(next)
                                bytes += nextBytes
                            }
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            if (!process.waitFor(LOGCAT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroy()
                out.appendLine("logcat collection timed out after ${LOGCAT_TIMEOUT_MS}ms")
            }
            reader.join(300)
            out.toString()
        }.getOrElse { "Failed to collect logcat: ${it.message ?: it.javaClass.simpleName}" }
    }

    private fun shouldKeepLogcatLine(line: String): Boolean =
        line.contains("Prism", ignoreCase = true) ||
            line.contains("yzddmr6", ignoreCase = true) ||
            line.contains("Shizuku", ignoreCase = true) ||
            line.contains("PackageInstaller", ignoreCase = true) ||
            line.contains("IntentResolver", ignoreCase = true) ||
            line.contains("DocumentsUI", ignoreCase = true) ||
            line.contains("FileExplorer", ignoreCase = true) ||
            line.contains("AndroidRuntime", ignoreCase = true) ||
            line.contains("Analytics", ignoreCase = true)

    private fun rollingFile(context: Context): File = File(exportDir(context), ROLLING_FILE)
    private fun exportDir(context: Context): File = File(context.filesDir, DIR)

    private fun cleanupOldExports(dir: File) {
        val exports = dir.listFiles { file -> file.name.startsWith(EXPORT_PREFIX) && file.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        exports.drop(MAX_EXPORT_FILES - 1).forEach { runCatching { it.delete() } }
    }

    private fun flush(timeoutMs: Long = 1_000L) {
        val latch = CountDownLatch(1)
        executor.execute { latch.countDown() }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }

    private fun timestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US)
        .apply { timeZone = TimeZone.getDefault() }
        .format(Date())

    private fun fileTimestamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())

    private fun currentUserId(): String =
        runCatching { Users.currentId().toString() }.getOrElse { Process.myUserHandle().hashCode().toString() }

    private fun versionName(context: Context): String =
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            "${info.versionName ?: "unknown"} ($code)"
        }.getOrDefault("unknown")
}
