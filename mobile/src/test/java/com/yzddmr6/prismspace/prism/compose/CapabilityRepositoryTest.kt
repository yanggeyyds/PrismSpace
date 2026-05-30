package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.CapabilityRepository
import com.yzddmr6.prismspace.prism.compose.vm.DefaultCapabilityRepository
import com.yzddmr6.prismspace.prism.compose.vm.ModeStore
import com.yzddmr6.prismspace.prism.compose.vm.PrismMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CapabilityRepositoryTest {
    /** In-memory store standing in for SharedPreferences (M4 persistence seam). */
    private class FakeModeStore(var stored: PrismMode? = null) : ModeStore {
        override fun load(): PrismMode? = stored
        override fun save(mode: PrismMode) { stored = mode }
    }

    @Test fun initialMode_isShizuku_whenShizukuAuthorized() {
        val repo = DefaultCapabilityRepository(shizukuAuthorized = { true })
        assertEquals(PrismMode.Shizuku, repo.selectedMode.value)
    }
    @Test fun initialMode_isNormal_whenShizukuNotAuthorized() {
        val repo = DefaultCapabilityRepository(shizukuAuthorized = { false })
        assertEquals(PrismMode.Normal, repo.selectedMode.value)
    }
    @Test fun setSelectedMode_publishes() {
        val repo: CapabilityRepository = DefaultCapabilityRepository(shizukuAuthorized = { false })
        repo.setSelectedMode(PrismMode.Root)
        assertEquals(PrismMode.Root, repo.selectedMode.value)
    }
    @Test fun oneSharedInstance_homeAndSettingsObserveSameFlow() {
        val repo: CapabilityRepository = DefaultCapabilityRepository(shizukuAuthorized = { false })
        val a = repo.selectedMode
        val b = repo.selectedMode
        assertSame(a, b)
        repo.setSelectedMode(PrismMode.Shizuku)
        assertEquals(PrismMode.Shizuku, a.value)
        assertEquals(PrismMode.Shizuku, b.value)
    }

    // M4 persistence ----------------------------------------------------------

    @Test fun persistedMode_winsOverDetection() {
        // User previously chose Root; even though Shizuku is now authorized, the stored choice loads.
        val store = FakeModeStore(stored = PrismMode.Root)
        val repo = DefaultCapabilityRepository(shizukuAuthorized = { true }, modeStore = store)
        assertEquals(PrismMode.Root, repo.selectedMode.value)
    }
    @Test fun setSelectedMode_writesThrough_toStore() {
        val store = FakeModeStore()
        val repo = DefaultCapabilityRepository(shizukuAuthorized = { false }, modeStore = store)
        repo.setSelectedMode(PrismMode.Shizuku)
        assertEquals(PrismMode.Shizuku, store.stored)
    }
}
