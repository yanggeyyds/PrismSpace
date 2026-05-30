package com.yzddmr6.prismspace.prism.compose.nav

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

object AppLaunchSignals {
    private val channel = Channel<Unit>(capacity = Channel.CONFLATED)
    val resetToHome: Flow<Unit> = channel.receiveAsFlow()
    fun signalResetToHome() { channel.trySend(Unit) }

    // "去启用" from the clone install-method selector (which lives in a Fragment, not the Compose
    // NavHost): jump to Settings and auto-open the run-mode guide so the user can enable Shizuku/Root.
    private val runModeChannel = Channel<Unit>(capacity = Channel.CONFLATED)
    val openRunMode: Flow<Unit> = runModeChannel.receiveAsFlow()
    fun signalOpenRunMode() { runModeChannel.trySend(Unit) }

    // Multi-select on the Space screen owns the whole screen (batch bar at top + per-app checks).
    // The global 4-tab bottom nav must hide so it doesn't compete with the batch context. SpaceScreen
    // mirrors its multi-select state here; PrismNavHost observes it to gate the bottom bar.
    private val _multiSelectActive = MutableStateFlow(false)
    val multiSelectActive: StateFlow<Boolean> = _multiSelectActive
    fun setMultiSelectActive(active: Boolean) { _multiSelectActive.value = active }
}
