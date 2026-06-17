package com.diary.app.ai

import com.diary.app.data.DiaryPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class InsightGeneratorTest {

    // --- pickInsightTypeExcluding ---

    @Test
    fun `pick type excludes the last type`() {
        val validTypes = setOf("mood", "encourage", "pattern", "greeting")

        repeat(20) {
            val picked = pickInsightTypeExcluding("mood")
            assertTrue("Expected valid type but got: $picked", picked in validTypes)
            assertTrue("Should not pick lastType", picked != "mood")
        }
    }

    @Test
    fun `pick type excludes encourage`() {
        repeat(20) {
            val picked = pickInsightTypeExcluding("encourage")
            assertTrue(picked != "encourage")
        }
    }

    @Test
    fun `pick type returns valid type when last type is empty`() {
        val validTypes = setOf("mood", "encourage", "pattern", "greeting")
        repeat(20) {
            val picked = pickInsightTypeExcluding("")
            assertTrue(picked in validTypes)
        }
    }

    @Test
    fun `pick type excludes pattern`() {
        repeat(20) {
            val picked = pickInsightTypeExcluding("pattern")
            assertTrue(picked != "pattern")
        }
    }

    @Test
    fun `pick type excludes greeting`() {
        repeat(20) {
            val picked = pickInsightTypeExcluding("greeting")
            assertTrue(picked != "greeting")
        }
    }

    // --- AiInsight data class ---

    @Test
    fun `ai insight stores text and type`() {
        val insight = AiInsight(text = "hello", type = "mood")

        assertEquals("hello", insight.text)
        assertEquals("mood", insight.type)
    }

    // --- generateLocalInsight - encourage type ---

    @Test
    fun `encourage insight for long-term user over 100 days`() {
        val daysAgo100 = daysBeforeEpoch(150)
        val entries = listOf(preview(1L, daysAgo100))

        val insight = generateLocalInsight(entries, "encourage")

        assertNotNull(insight)
        assertEquals("encourage", insight!!.type)
        assertEquals("已经坚持了这么久，真好", insight.text)
    }

    @Test
    fun `encourage insight for user over 30 days`() {
        val daysAgo50 = daysBeforeEpoch(50)
        val entries = listOf(preview(1L, daysAgo50))

        val insight = generateLocalInsight(entries, "encourage")

        assertNotNull(insight)
        assertEquals("一个月了，你的坚持有了重量", insight!!.text)
    }

    @Test
    fun `encourage insight for user with many entries`() {
        val entries = (1L..50L).map { preview(it, daysBeforeEpoch(5)) }

        val insight = generateLocalInsight(entries, "encourage")

        assertNotNull(insight)
        assertEquals("不知不觉，已经写了这么多", insight!!.text)
    }

    @Test
    fun `encourage insight fallback for new user`() {
        val entries = listOf(preview(1L, daysBeforeEpoch(3)))

        val insight = generateLocalInsight(entries, "encourage")

        assertNotNull(insight)
        assertEquals("每一天的记录都值得", insight!!.text)
    }

    // --- generateLocalInsight - mood type ---

    @Test
    fun `mood insight for high recent mood`() {
        val recent = daysBeforeEpoch(1)
        val entries = listOf(
            previewWithMood(1L, recent, 5),
            previewWithMood(2L, recent, 4)
        )

        val insight = generateLocalInsight(entries, "mood")

        assertNotNull(insight)
        assertEquals("mood", insight!!.type)
        assertEquals("最近状态不错，继续保持", insight.text)
    }

    @Test
    fun `mood insight for low recent mood`() {
        val recent = daysBeforeEpoch(1)
        val entries = listOf(
            previewWithMood(1L, recent, 1),
            previewWithMood(2L, recent, 2)
        )

        val insight = generateLocalInsight(entries, "mood")

        assertNotNull(insight)
        assertEquals("低落的时候，写下来也是一种力量", insight!!.text)
    }

    @Test
    fun `mood insight fallback when no mood data`() {
        val entries = listOf(preview(1L, daysBeforeEpoch(1)))

        val insight = generateLocalInsight(entries, "mood")

        assertNotNull(insight)
        assertEquals("今天也来写点什么吧", insight!!.text)
    }

    @Test
    fun `mood insight fallback for neutral mood`() {
        val recent = daysBeforeEpoch(1)
        val entries = listOf(previewWithMood(1L, recent, 3))

        val insight = generateLocalInsight(entries, "mood")

        assertNotNull(insight)
        assertEquals("今天也来写点什么吧", insight!!.text)
    }

    // --- generateLocalInsight - greeting type ---

    @Test
    fun `greeting insight returns a valid insight`() {
        val entries = listOf(preview(1L, daysBeforeEpoch(1)))

        val insight = generateLocalInsight(entries, "greeting")

        assertNotNull(insight)
        assertEquals("greeting", insight!!.type)
        assertTrue(insight.text.isNotBlank())
    }

    // --- generateLocalInsight - unknown type ---

    @Test
    fun `unknown type returns null`() {
        val entries = listOf(preview(1L, daysBeforeEpoch(1)))

        assertNull(generateLocalInsight(entries, "unknown"))
    }

    @Test
    fun `empty entries returns null for unknown type`() {
        assertNull(generateLocalInsight(emptyList(), "unknown"))
    }

    // --- generateLocalInsight with old entries (not recent) ---

    @Test
    fun `mood insight with only old entries falls back to default`() {
        val oldDate = daysBeforeEpoch(30)
        val entries = listOf(previewWithMood(1L, oldDate, 5))

        val insight = generateLocalInsight(entries, "mood")

        assertNotNull(insight)
        // Old entries are not in recent 7-day window, so recentMoods is empty
        assertEquals("今天也来写点什么吧", insight!!.text)
    }

    // helpers

    private fun preview(id: Long, createdAt: Long) = DiaryPreview(
        id = id, title = "", plainText = "", moodLevel = null,
        weather = null, location = null, latitude = null, longitude = null,
        isFavorite = false, createdAt = createdAt, updatedAt = createdAt
    )

    private fun previewWithMood(id: Long, createdAt: Long, mood: Int) = DiaryPreview(
        id = id, title = "", plainText = "", moodLevel = mood,
        weather = null, location = null, latitude = null, longitude = null,
        isFavorite = false, createdAt = createdAt, updatedAt = createdAt
    )

    private fun daysBeforeEpoch(days: Long): Long {
        val target = LocalDate.now().minusDays(days)
        return target.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
