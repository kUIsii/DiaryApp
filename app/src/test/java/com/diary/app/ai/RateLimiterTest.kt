package com.diary.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    // --- canMakeRequestInternal ---

    @Test
    fun `allows request when both totals are zero`() {
        assertTrue(canMakeRequestInternal(dailyTotal = 0, modelCount = 0))
    }

    @Test
    fun `allows request when under both limits`() {
        assertTrue(canMakeRequestInternal(dailyTotal = 100, modelCount = 50))
    }

    @Test
    fun `blocks request when daily total reaches limit`() {
        assertFalse(canMakeRequestInternal(dailyTotal = 2000, modelCount = 0))
    }

    @Test
    fun `blocks request when daily total exceeds limit`() {
        assertFalse(canMakeRequestInternal(dailyTotal = 2001, modelCount = 0))
    }

    @Test
    fun `blocks request when model count reaches limit`() {
        assertFalse(canMakeRequestInternal(dailyTotal = 0, modelCount = 200))
    }

    @Test
    fun `blocks request when model count exceeds limit`() {
        assertFalse(canMakeRequestInternal(dailyTotal = 0, modelCount = 201))
    }

    @Test
    fun `blocks request when both limits exceeded`() {
        assertFalse(canMakeRequestInternal(dailyTotal = 2000, modelCount = 200))
    }

    @Test
    fun `allows request one below daily limit`() {
        assertTrue(canMakeRequestInternal(dailyTotal = 1999, modelCount = 0))
    }

    @Test
    fun `allows request one below model limit`() {
        assertTrue(canMakeRequestInternal(dailyTotal = 0, modelCount = 199))
    }

    @Test
    fun `respects custom daily limit`() {
        assertTrue(canMakeRequestInternal(dailyTotal = 5, modelCount = 0, dailyLimit = 10))
        assertFalse(canMakeRequestInternal(dailyTotal = 10, modelCount = 0, dailyLimit = 10))
    }

    @Test
    fun `respects custom model limit`() {
        assertTrue(canMakeRequestInternal(dailyTotal = 0, modelCount = 3, modelLimit = 5))
        assertFalse(canMakeRequestInternal(dailyTotal = 0, modelCount = 5, modelLimit = 5))
    }

    @Test
    fun `blocks when daily limit is zero`() {
        assertFalse(canMakeRequestInternal(dailyTotal = 0, modelCount = 0, dailyLimit = 0))
    }

    // --- UsageStats defaults ---

    @Test
    fun `usage stats has correct default limits`() {
        val stats = RateLimiter.UsageStats(
            dailyTotal = 0,
            modelUsage = emptyMap()
        )

        assertEquals(2000, stats.dailyLimit)
        assertEquals(200, stats.modelLimit)
    }

    @Test
    fun `usage stats tracks model usage map`() {
        val usage = mapOf("model-a" to 10, "model-b" to 20)
        val stats = RateLimiter.UsageStats(
            dailyTotal = 30,
            modelUsage = usage
        )

        assertEquals(30, stats.dailyTotal)
        assertEquals(10, stats.modelUsage["model-a"])
        assertEquals(20, stats.modelUsage["model-b"])
    }
}
