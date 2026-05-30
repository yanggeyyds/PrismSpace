package com.yzddmr6.prismspace.analytics

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DiagnosticLogWindowTest {

    @Test fun trimKeepsInputWhenAlreadyUnderLimit() {
        val input = ByteArray(128) { it.toByte() }

        val output = DiagnosticLog.trimToWindowBytes(input, DiagnosticLog.MAX_ROLLING_BYTES)

        assertSame(input, output)
    }

    @Test fun trimCapsLogToTwoMiBAndKeepsNewestBytes() {
        val input = ByteArray(DiagnosticLog.MAX_ROLLING_BYTES + 4096) { (it % 251).toByte() }

        val output = DiagnosticLog.trimToWindowBytes(input, DiagnosticLog.MAX_ROLLING_BYTES)

        assertEquals(DiagnosticLog.MAX_ROLLING_BYTES, output.size)
        assertArrayEquals(
            DiagnosticLog.TRIM_MARKER,
            output.copyOfRange(0, DiagnosticLog.TRIM_MARKER.size),
        )
        assertArrayEquals(
            input.copyOfRange(input.size - (DiagnosticLog.MAX_ROLLING_BYTES - DiagnosticLog.TRIM_MARKER.size), input.size),
            output.copyOfRange(DiagnosticLog.TRIM_MARKER.size, output.size),
        )
    }

    @Test fun trimCanKeepHeadroomBelowTwoMiBBudget() {
        val targetBytes = DiagnosticLog.MAX_ROLLING_BYTES - 128 * 1024
        val input = ByteArray(DiagnosticLog.MAX_ROLLING_BYTES + 4096) { (it % 251).toByte() }

        val output = DiagnosticLog.trimToWindowBytes(input, targetBytes)

        assertEquals(targetBytes, output.size)
        assertArrayEquals(
            input.copyOfRange(input.size - (targetBytes - DiagnosticLog.TRIM_MARKER.size), input.size),
            output.copyOfRange(DiagnosticLog.TRIM_MARKER.size, output.size),
        )
    }
}
