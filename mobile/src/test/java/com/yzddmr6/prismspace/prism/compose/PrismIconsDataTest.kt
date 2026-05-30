package com.yzddmr6.prismspace.prism.compose

import androidx.compose.ui.graphics.vector.PathParser
import com.yzddmr6.prismspace.prism.compose.component.PrismIconPaths
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismIconsDataTest {
    @Test fun everyPathParsesAndIsNonBlank() {
        assertTrue("expected >=34 icon paths", PrismIconPaths.D.size >= 34)
        PrismIconPaths.D.forEach { (k, d) ->
            assertTrue("blank d for $k", d.isNotBlank())
            val nodes = PathParser().parsePathString(d).toNodes()
            assertTrue("no path nodes for $k", nodes.isNotEmpty())
        }
    }
    @Test fun coreKeysPresent() {
        listOf("home","grid","files","set","search","dots","add","check","chev",
            "close","key","info","shield","wrench","download","refresh","sort","swaph","alert")
            .forEach { assertTrue("missing key $it", PrismIconPaths.D.containsKey(it)) }
    }
}
