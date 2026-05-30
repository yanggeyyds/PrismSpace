package com.yzddmr6.prismprobe

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var logView: TextView
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        appendLog("ready")
        appendLog(readState().identityLine())
        if (intent?.action == Intent.ACTION_SEND) appendLog("intent=send ${intent.type.orEmpty()}")
    }

    private fun buildContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        content.addView(title("PrismProbe"))
        content.addView(section("环境"))
        content.addView(action("刷新环境") { appendLog(readState().identityLine()) })
        content.addView(section("权限"))
        content.addView(action("检查权限") { appendLog(permissionLine()) })
        content.addView(action("请求权限") { requestSensitivePermissions() })
        content.addView(section("文件"))
        content.addView(action("检查文件可见性") { appendLog(readFileState().fileVisibilityLine()) })
        content.addView(action("创建测试文件") { appendLog(createProbeFile()) })
        content.addView(action("打开文件") { openFilePicker() })
        content.addView(section("通知"))
        content.addView(action("发送通知") { appendLog(ProbeNotification.send(this)) })
        content.addView(action("取消通知") { appendLog(ProbeNotification.cancel(this)) })
        content.addView(section("后台"))
        content.addView(action("启动后台任务") { appendLog(startProbeService()) })
        content.addView(section("网络"))
        content.addView(action("访问网络") { checkNetwork() })
        content.addView(section("Intent"))
        content.addView(action("分享文本") { shareText() })
        content.addView(action("打开图片") { openImageIntent() })
        logView = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(16) }
        }
        content.addView(logView)
        return ScrollView(this).apply { addView(content) }
    }

    private fun readState(): ProbeState {
        val uid = Process.myUid()
        return ProbeState(
            userId = uid / 100000,
            uid = uid,
            packageName = packageName,
            processName = Application.getProcessName(),
            sdkInt = Build.VERSION.SDK_INT,
            isManagedProfile = uid / 100000 > 0,
            cameraGranted = isGranted(Manifest.permission.CAMERA),
            microphoneGranted = isGranted(Manifest.permission.RECORD_AUDIO),
            locationGranted = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
            contactsGranted = isGranted(Manifest.permission.READ_CONTACTS),
            notificationGranted = Build.VERSION.SDK_INT < 33 || isGranted(Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    private fun readFileState(): ProbeState {
        val base = readState()
        return base.copy(
            visibleImageCount = queryCount(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            visibleDownloadCount = queryCount(MediaStore.Downloads.EXTERNAL_CONTENT_URI),
        )
    }

    private fun queryCount(uri: Uri): Int =
        runCatching {
            contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null).useCount()
        }.getOrDefault(-1)

    private fun Cursor?.useCount(): Int {
        if (this == null) return -1
        use { cursor -> return cursor.count }
    }

    private fun permissionLine(): String = readState().permissionLine()

    private fun isGranted(permission: String): Boolean =
        Build.VERSION.SDK_INT < 23 ||
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestSensitivePermissions() {
        if (Build.VERSION.SDK_INT < 23) {
            appendLog("permission=request skipped sdk<23")
            return
        }
        val permissions = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS)
        appendLog("permission=requested")
    }

    private fun createProbeFile(): String =
        runCatching {
            val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return "file=failed external-dir-missing"
            val file = File(dir, "prismprobe-${System.currentTimeMillis()}.txt")
            file.writeText("PrismProbe ${Date()}\n")
            "file=created ${file.absolutePath}"
        }.getOrElse { error ->
            "file=failed ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
        }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        runCatching { startActivityForResult(intent, REQUEST_OPEN_FILE) }
            .onSuccess { appendLog("file=open-picker") }
            .onFailure { appendLog("file=open-picker failed ${it.javaClass.simpleName}") }
    }

    private fun startProbeService(): String =
        runCatching {
            startService(Intent(this, ProbeService::class.java))
            "service=started"
        }.getOrElse { error ->
            "service=failed ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
        }

    private fun checkNetwork() {
        appendLog("network=running")
        thread(name = "PrismProbeNetwork") {
            val result = runCatching {
                val connection = URL("https://connectivitycheck.gstatic.com/generate_204")
                    .openConnection() as HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.requestMethod = "GET"
                "network=http-${connection.responseCode}"
            }.getOrElse { error ->
                "network=failed ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            }
            runOnUiThread { appendLog(result) }
        }
    }

    private fun shareText() {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "PrismProbe share test")
        startActivity(Intent.createChooser(send, "PrismProbe"))
        appendLog("intent=share")
    }

    private fun openImageIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        runCatching { startActivity(intent) }
            .onSuccess { appendLog("intent=image-picker") }
            .onFailure { appendLog("intent=image-picker failed ${it.javaClass.simpleName}") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSIONS) return
        val granted = grantResults.count { it == PackageManager.PERMISSION_GRANTED }
        appendLog("permission=result granted=$granted total=${permissions.size}")
        appendLog(permissionLine())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_OPEN_FILE) return
        appendLog("file=open-result result=$resultCode uri=${data?.data?.scheme.orEmpty()}")
    }

    private fun section(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 18f
        setPadding(0, dp(22), 0, dp(8))
    }

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 28f
        gravity = Gravity.START
    }

    private fun action(text: String, listener: () -> Unit): Button = Button(this).apply {
        this.text = text
        setOnClickListener { listener() }
    }

    private fun appendLog(message: String) {
        val line = "${timeFormat.format(Date())} $message"
        logView.text = if (logView.text.isNullOrBlank()) line else "${logView.text}\n$line"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        private const val REQUEST_PERMISSIONS = 6107
        private const val REQUEST_OPEN_FILE = 6108
    }
}
