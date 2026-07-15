package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.compose.vm.PrismMode
import com.yzddmr6.prismspace.prism.compose.vm.prismModeLabelRes
import org.junit.Assert.assertEquals
import org.junit.Test

/** Run-mode labels moved to string resources (en + zh); verify the enum→resId mapping. */
class PrismModeLabelTest {
    @Test fun normal() { assertEquals(R.string.lz_mode_normal, prismModeLabelRes(PrismMode.Normal)) }
    @Test fun shizuku() { assertEquals(R.string.lz_mode_shizuku, prismModeLabelRes(PrismMode.Shizuku)) }
    @Test fun dhizuku() { assertEquals(R.string.lz_mode_dhizuku, prismModeLabelRes(PrismMode.Dhizuku)) }
    @Test fun root() { assertEquals(R.string.lz_mode_root, prismModeLabelRes(PrismMode.Root)) }
}
