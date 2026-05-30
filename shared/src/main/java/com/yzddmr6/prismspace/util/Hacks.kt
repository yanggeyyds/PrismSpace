package com.yzddmr6.prismspace.util

import android.os.UserManager

fun UserManager.getProfileIds(userId: Int, enabledOnly: Boolean): IntArray? = Hacks.UserManager_getProfileIds.invoke(userId, enabledOnly).on(this)
