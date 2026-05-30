package com.yzddmr6.prismspace.common.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel

abstract class BaseAndroidViewModel(app: Application): AndroidViewModel(app) {

	abstract val tag: String
}