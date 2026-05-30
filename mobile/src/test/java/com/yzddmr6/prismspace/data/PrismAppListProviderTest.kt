package com.yzddmr6.prismspace.data

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismAppListProviderTest {

    @Test fun visibleAppsRemainWhenProfileQueryReturnsEmpty() {
        val visibleTwitter = app("com.twitter.android")

        val merged = mergeAppsByPackage(emptySequence(), sequenceOf(visibleTwitter)).toList()

        assertSame(visibleTwitter, merged.single())
    }

    @Test fun profileQueryOverridesVisibleFallbackForSamePackage() {
        val visibleTwitter = app("com.twitter.android")
        val profileTwitter = app("com.twitter.android")

        val merged = mergeAppsByPackage(sequenceOf(profileTwitter), sequenceOf(visibleTwitter)).toList()

        assertSame(profileTwitter, merged.single())
    }

    @Test fun noFallbackKeepsEmptyResultEmpty() {
        assertTrue(mergeAppsByPackage(emptySequence(), emptySequence()).toList().isEmpty())
    }

    private fun app(pkg: String) = ApplicationInfo().apply { packageName = pkg }
}
