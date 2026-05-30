package com.yzddmr6.prismspace.analytics

import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.annotation.Size
import com.yzddmr6.prismspace.PrismApplication
import org.intellij.lang.annotations.Pattern

/** Local event and crash logging facade. */
interface Analytics {

	interface Event {
		@CheckResult fun with(key: Param, value: String?) = withRaw(key.key, value)
		@CheckResult fun withRaw(key: String, value: String?): Event
		fun send()
	}

	enum class Param(@param:Pattern("^[a-zA-Z][a-zA-Z0-9_]*$") val key: String) {
		ITEM_ID("item_id"),
		ITEM_NAME("item_name"),
		ITEM_CATEGORY("item_category"),
		LOCATION("location"),
		CONTENT("content");
	}

	@CheckResult fun event(@Size(min = 1, max = 40) @Pattern("^[a-zA-Z][a-zA-Z0-9_]*$") event: String): Event
	fun reportEvent(event: String, params: Bundle)
	fun trace(key: String, value: String): Analytics
	fun trace(key: String, value: Int): Analytics
	fun trace(key: String, value: Boolean): Analytics
	fun report(t: Throwable)
	fun report(message: String, t: Throwable)
	fun logAndReport(tag: String, message: String, t: Throwable) { DiagnosticLog.e(tag, message, t); report(message, t) }

	enum class Property(val key: String) {
		DeviceOwner("device_owner"),
		PrismSetup("prism_setup"),
		RemoteConfigAvailable("remote_config_avail"),
	}

	fun setProperty(property: Property, @Size(max = 36) value: String)
	fun setProperty(property: Property, value: Boolean): Analytics = this.also { setProperty(property, value.toString()) }

	companion object {
		@JvmStatic fun log(tag: String, message: String) { DiagnosticLog.i(tag, message); CrashReport.log("[$tag] $message") }
		@JvmStatic @Suppress("FunctionName") fun `$`() = impl
		operator fun invoke() = impl
		private val impl: Analytics = AnalyticsImpl(PrismApplication.get())
	}
}

fun analytics(): Analytics = Analytics()
