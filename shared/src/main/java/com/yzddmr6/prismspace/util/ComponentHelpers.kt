package com.yzddmr6.prismspace.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP

fun ComponentInfo.getComponentName() = ComponentName(packageName, name)

inline fun <reified T> Context.enableComponent() =
		packageManager.setComponentEnabledSetting(ComponentName(this, T::class.java), COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
