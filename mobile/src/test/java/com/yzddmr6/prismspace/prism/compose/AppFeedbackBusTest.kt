package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.ActionFeedback
import com.yzddmr6.prismspace.prism.compose.vm.AppFeedbackBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFeedbackBusTest {
    @After fun tearDown() { AppFeedbackBus.reset() }

    @Test fun emitThenConsume() {
        assertNull(AppFeedbackBus.feedback.value)
        AppFeedbackBus.emit(ActionFeedback("hi", false))
        assertEquals(ActionFeedback("hi", false), AppFeedbackBus.feedback.value)
        AppFeedbackBus.consume()
        assertNull(AppFeedbackBus.feedback.value)
    }
    @Test fun resetClears() {
        AppFeedbackBus.emit(ActionFeedback("x", true))
        AppFeedbackBus.reset()
        assertNull(AppFeedbackBus.feedback.value)
    }
}
