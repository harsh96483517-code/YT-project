package com.tensei.ytxtube

import java.io.IOException

/**
 * Pure decision logic for retrying read-only API calls (kept free of Android classes so it can be
 * unit-tested). Used by [NewPipeDownloader]; downloads and media streaming have their own policies.
 */
object RetryPolicy {
    const val MAX_RETRIES = 2
    private const val MAX_WAIT_MS = 8_000L
    private const val BASE_WAIT_MS = 500L

    /** Rate limits and temporary server failures are worth another attempt. */
    fun isRetryableStatus(code: Int): Boolean =
        code == 429 || code == 500 || code == 502 || code == 503 || code == 504

    /** Dropped connections and timeouts are retryable; anything that is not an I/O problem is not. */
    fun isRetryableError(t: Throwable): Boolean = t is IOException

    /**
     * How long to wait before retry number [attempt] (0-based). A numeric `Retry-After` header
     * (seconds) from the server wins, otherwise 0.5s, 1s, 2s... Always capped so a request never
     * hangs for long.
     */
    fun delayMs(attempt: Int, retryAfterHeader: String?): Long {
        val fromHeader = retryAfterHeader?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.times(1000L)
        val wait = fromHeader ?: (BASE_WAIT_MS shl attempt.coerceIn(0, 10))
        return wait.coerceAtMost(MAX_WAIT_MS)
    }
}
