package com.tensei.ytxtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RetryPolicyTest {
    @Test fun rateLimitAndServerErrorsAreRetried() {
        listOf(429, 500, 502, 503, 504).forEach { assertTrue(RetryPolicy.isRetryableStatus(it)) }
    }

    @Test fun clientErrorsAndSuccessAreNotRetried() {
        listOf(200, 204, 400, 401, 403, 404).forEach { assertFalse(RetryPolicy.isRetryableStatus(it)) }
    }

    @Test fun ioProblemsAreRetriedOthersAreNot() {
        assertTrue(RetryPolicy.isRetryableError(IOException("reset")))
        assertTrue(RetryPolicy.isRetryableError(SocketTimeoutException()))
        assertFalse(RetryPolicy.isRetryableError(IllegalStateException("bug")))
    }

    @Test fun backoffDoublesAndIsCapped() {
        assertEquals(500L, RetryPolicy.delayMs(0, null))
        assertEquals(1000L, RetryPolicy.delayMs(1, null))
        assertEquals(2000L, RetryPolicy.delayMs(2, null))
        assertEquals(8000L, RetryPolicy.delayMs(10, null))
    }

    @Test fun retryAfterHeaderIsHonouredButCapped() {
        assertEquals(3000L, RetryPolicy.delayMs(0, "3"))
        assertEquals(8000L, RetryPolicy.delayMs(0, "120"))
        assertEquals(500L, RetryPolicy.delayMs(0, "Wed, 21 Oct 2026 07:28:00 GMT"))
    }
}
