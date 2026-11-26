package com.tensei.ytxtube

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test fun newerPatchIsDetected() = assertTrue(isNewerVersion("2.3.1", "2.3.0"))
    @Test fun sameVersionIsNotNewer() = assertFalse(isNewerVersion("v2.3.0", "2.3.0"))
    @Test fun olderIsNotNewer() = assertFalse(isNewerVersion("2.2.9", "2.3.0"))
    @Test fun shorterVersionPadsWithZero() = assertFalse(isNewerVersion("2.3", "2.3.0"))
    @Test fun twoDigitMinorBeatsOneDigit() = assertTrue(isNewerVersion("2.10.0", "2.9.0"))
}
