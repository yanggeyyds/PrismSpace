package com.yzddmr6.prismspace.prism.service

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossProfileAccessPromptTest {

    @Test fun directProfileLaunchNeedsPromptOnAndroidRAndAboveWhenNotAllowed() {
        assertTrue(CrossProfileAccessPrompt.shouldPrompt(Build.VERSION_CODES.R, canInteract = false, canRequest = true))
    }

    @Test fun directProfileLaunchDoesNotPromptWhenAlreadyAllowed() {
        assertFalse(CrossProfileAccessPrompt.shouldPrompt(Build.VERSION_CODES.R, canInteract = true, canRequest = true))
    }

    @Test fun directProfileLaunchDoesNotPromptWhenSystemCannotRequest() {
        assertFalse(CrossProfileAccessPrompt.shouldPrompt(Build.VERSION_CODES.R, canInteract = false, canRequest = false))
    }
}
