package com.yzddmr6.prismspace.prism.compose.vm

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the user-selected mode. Home and
 * Settings both read [selectedMode] from one process singleton, so they
 * can never display contradicting modes.
 *
 * The selected mode is persisted via [ModeStore] so a user's
 * Shizuku/Root choice survives app restarts instead of silently reverting
 * to detection-only each launch.
 */
interface CapabilityRepository {
    val selectedMode: StateFlow<PrismMode>
    fun setSelectedMode(mode: PrismMode)
}

/**
 * Persistence seam for the selected mode. Kept as an interface so tests and
 * any non-persisted construction can stay in-memory ([NoopModeStore]).
 */
interface ModeStore {
    fun load(): PrismMode?
    fun save(mode: PrismMode)
}

/** In-memory no-op store — used by tests and any non-persisted construction. */
object NoopModeStore : ModeStore {
    override fun load(): PrismMode? = null
    override fun save(mode: PrismMode) {}
}

/** Default SharedPreferences-backed store (credential-protected — mirrors [ExperimentalFlags]). */
class SharedPrefsModeStore(context: Context) : ModeStore {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    override fun load(): PrismMode? =
        prefs.getString(KEY_SELECTED_MODE, null)
            ?.let { name -> PrismMode.values().firstOrNull { it.name == name } }
    override fun save(mode: PrismMode) {
        prefs.edit().putString(KEY_SELECTED_MODE, mode.name).apply()
    }
    private companion object { const val KEY_SELECTED_MODE = "prism_selected_mode" }
}

class DefaultCapabilityRepository(
    private val shizukuAuthorized: () -> Boolean = { ShizukuUtil.isAuthorized() },
    private val dhizukuAuthorized: () -> Boolean = { false },
    private val modeStore: ModeStore = NoopModeStore,
) : CapabilityRepository {
    // Persisted choice wins; otherwise fall back to the original detect-once default.
    private val _selectedMode = MutableStateFlow(
        modeStore.load() ?: when {
            shizukuAuthorized() -> PrismMode.Shizuku
            dhizukuAuthorized() -> PrismMode.Dhizuku
            else -> PrismMode.Normal
        }
    )
    override val selectedMode: StateFlow<PrismMode> = _selectedMode
    override fun setSelectedMode(mode: PrismMode) {
        _selectedMode.value = mode
        modeStore.save(mode)
    }
}

/** Manual DI seam matching the repository-provider pattern. */
object CapabilityRepositoryProvider {
    @Volatile private var instance: CapabilityRepository? = null
    fun get(context: Context): CapabilityRepository =
        instance ?: synchronized(this) {
            instance ?: DefaultCapabilityRepository(
                dhizukuAuthorized = { DhizukuUtil.isAuthorized(context.applicationContext) },
                modeStore = SharedPrefsModeStore(context),
            ).also { instance = it }
        }
    @VisibleForTesting fun setForTest(repo: CapabilityRepository?) { instance = repo }
}
