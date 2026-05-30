package com.yzddmr6.prismspace.prism.compose.space

import android.content.Context
import androidx.annotation.VisibleForTesting

/** Manual DI seam — no Hilt. VMs default to this; tests inject a fake. */
object SpaceRepositoryProvider {
    @Volatile private var instance: SpaceRepository? = null
    fun get(context: Context): SpaceRepository =
        instance ?: synchronized(this) {
            instance ?: DefaultSpaceRepository(context.applicationContext).also { instance = it }
        }
    /** Test-only override hook. Always call `setForTest(null)` in `@After` to reset
     *  this singleton between tests, otherwise a fake leaks into later tests. */
    @VisibleForTesting fun setForTest(repo: SpaceRepository?) { instance = repo }
}
