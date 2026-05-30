package com.yzddmr6.prismspace.prism.compose.vm

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-scoped feedback channel. ViewModels publish [ActionFeedback] here;
 * the single SnackbarHost in PrismNavHost consumes it.
 */
object AppFeedbackBus {
    private val _feedback = MutableStateFlow<ActionFeedback?>(null)
    val feedback: StateFlow<ActionFeedback?> = _feedback

    fun emit(f: ActionFeedback) { _feedback.value = f }
    fun consume() { _feedback.value = null }

    @VisibleForTesting fun reset() { _feedback.value = null }
}
