package com.yzddmr6.prismprobe

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ProbeService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null
}
