package com.yzddmr6.prismprobe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object ProbeNotification {

    private const val CHANNEL_ID = "prismprobe"
    private const val NOTIFICATION_ID = 6106

    fun send(context: Context): String {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return "notification-manager=missing"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "PrismProbe", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PrismProbe")
            .setContentText("notification test")
            .setAutoCancel(true)
            .build()
        return runCatching {
            manager.notify(NOTIFICATION_ID, notification)
            "notification=sent"
        }.getOrElse { error ->
            "notification=failed ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
        }
    }

    fun cancel(context: Context): String {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        return "notification=cancelled"
    }
}
