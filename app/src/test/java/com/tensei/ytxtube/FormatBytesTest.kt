package com.tensei.ytxtube

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {
    @Test fun zeroOrNegativeShowsZeroMb() {
        assertEquals("0 MB", formatBytes(0))
        assertEquals("0 MB", formatBytes(-5))
    }

    @Test fun smallSizesUseKilobytes() = assertEquals("512 KB", formatBytes(512L * 1024))

    @Test fun mediumSizesKeepOneDecimal() = assertEquals("1.5 MB", formatBytes((1.5 * 1024 * 1024).toLong()))

    @Test fun tenMegabytesAndUpDropTheDecimal() = assertEquals("25 MB", formatBytes(25L * 1024 * 1024))

    @Test fun gigabytesShowTwoDecimals() = assertEquals("2.00 GB", formatBytes(2L * 1024 * 1024 * 1024))
}
