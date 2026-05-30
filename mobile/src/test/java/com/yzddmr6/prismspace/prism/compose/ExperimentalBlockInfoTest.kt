package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.space.experimentalBlockInfo
import com.yzddmr6.prismspace.prism.compose.vm.StringResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The honest experimental-block copy now lives in string resources (en + zh). This test verifies
 * the function threads the runtime args (space name / user id / dual count) into the body via the
 * resolver, locale-agnostically.
 */
class ExperimentalBlockInfoTest {
    private val fake: StringResolver = { id, args ->
        when (id) {
            R.string.lz_space_exp_title -> "TITLE"
            R.string.lz_space_exp_dismiss -> "DISMISS"
            R.string.lz_space_exp_body -> "name=${args[0]} user=${args[1]} count=${args[2]}"
            else -> ""
        }
    }

    @Test fun `experimental block threads runtime args into the body`() {
        val info = experimentalBlockInfo("双开空间", 11, 1, fake)
        assertEquals("TITLE", info.title)
        assertEquals("DISMISS", info.dismiss)
        assertTrue(info.body.contains("name=双开空间 user=11 count=1"))
    }
}
