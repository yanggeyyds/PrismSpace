package com.yzddmr6.prismspace

import android.app.Application
import com.yzddmr6.prismspace.analytics.CrashReport
import com.yzddmr6.prismspace.analytics.DiagnosticLog

/**
 * For singleton instance purpose only.
 *
 * Created by Oasis on 2018/1/3.
 */
class PrismApplication : Application() {

	companion object {
		@JvmStatic fun get() = sInstance

		lateinit var sInstance: PrismApplication
	}

	init {
		sInstance = this
		CrashReport.initCrashHandler()
	}

	override fun onCreate() {
		super.onCreate()
		DiagnosticLog.init(this)
		CrashReport.initCrashHandler()
	}
}
