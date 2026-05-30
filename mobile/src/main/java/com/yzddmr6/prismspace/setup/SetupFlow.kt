package com.yzddmr6.prismspace.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import com.yzddmr6.prismspace.util.Users
import com.yzddmr6.prismspace.util.Users.Companion.toId

object SetupFlow {
    private const val TAG = "Prism.SetupFlow"

    fun open(context: Context) {
        val appContext = context.applicationContext
        runCatching { Users.refreshUsers(appContext) }
            .onFailure { DiagnosticLog.w(TAG, "refresh users before setup failed", it) }
        DiagnosticLog.i(TAG, "open setup flow profile=${Users.profile?.toId() ?: Users.NULL_ID}")
        val intent = Intent(context, SetupActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
