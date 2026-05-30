package com.yzddmr6.prismspace.shizuku

import rikka.shizuku.ShizukuProvider

class NonRootShizukuProvider: ShizukuProvider() {

    override fun onCreate() = disableAutomaticSuiInitialization().run { super.onCreate() }
}