package com.yzddmr6.prismspace.setup.compose

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Retained state for [com.yzddmr6.prismspace.setup.SetupActivity].
 *
 * Survives configuration changes (rotation, dark-mode toggle) via the standard
 * ViewModelStore. Does NOT survive process death — `kotlin-parcelize` is not
 * applied to `:mobile` so we cannot Parcel the sealed [SetupUiState] into a
 * [androidx.lifecycle.SavedStateHandle] without a manual encoder. Process-death
 * survival can be added later with a manual encoder.
 *
 * This is intentionally distinct from
 * [com.yzddmr6.prismspace.setup.SetupViewModel], which holds provisioning
 * business logic rather than lifecycle UI state.
 */
class SetupStateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Welcome)
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /** Sticky across rotation. */
    var incompleteSetupAcked: Boolean = false

    fun setUiState(next: SetupUiState) {
        _uiState.value = next
    }
}
