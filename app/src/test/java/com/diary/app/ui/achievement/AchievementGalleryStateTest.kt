package com.diary.app.ui.achievement

import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementDef
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementState
import com.diary.app.data.AchievementStats
import com.diary.app.data.AchievementTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementGalleryStateTest {

    @Test
    fun recent_unlocks_returns_newest_unlocked_items_first() {
        val oldest = achievementItem(
            key = "oldest",
            unlocked = true,
            unlockedAt = 1_000L
        )
        val newest = achievementItem(
            key = "newest",
            unlocked = true,
            unlockedAt = 5_000L
        )
        val locked = achievementItem(
            key = "locked",
            progress = 4,
            target = 10
        )

        val recent = recentUnlocks(
            items = listOf(oldest, locked, newest),
            limit = 2
        )

        assertEquals(listOf("newest", "oldest"), recent.map { it.def.key })
    }

    @Test
    fun near_completion_prioritizes_locked_items_with_highest_progress() {
        val almostThere = achievementItem(
            key = "almost",
            progress = 9,
            target = 10
        )
        val midway = achievementItem(
            key = "midway",
            progress = 7,
            target = 10
        )
        val tooEarly = achievementItem(
            key = "early",
            progress = 2,
            target = 10
        )
        val alreadyUnlocked = achievementItem(
            key = "done",
            unlocked = true,
            unlockedAt = 3_000L,
            progress = 10,
            target = 10
        )

        val nearCompletion = nearCompletionItems(
            items = listOf(tooEarly, alreadyUnlocked, midway, almostThere),
            limit = 3,
            minimumProgressFraction = 0.6f
        )

        assertEquals(listOf("almost", "midway"), nearCompletion.map { it.def.key })
    }

    @Test
    fun hidden_filter_keeps_locked_hidden_items_discoverable_without_revealing_content() {
        val hiddenLocked = achievementItem(
            key = "hidden_locked",
            name = "午夜来信",
            description = "在特别的日子里写下日记",
            hidden = true,
            progress = 0,
            target = 1
        )
        val visibleUnlocked = achievementItem(
            key = "visible_unlocked",
            unlocked = true,
            unlockedAt = 1_500L
        )

        val cards = filterAchievementGalleryCards(
            items = listOf(hiddenLocked, visibleUnlocked),
            stateFilter = AchievementGalleryFilter.HIDDEN
        )

        assertEquals(1, cards.size)
        assertEquals("未公开藏品", cards.first().title)
        assertTrue(cards.first().isConcealed)
        assertTrue(cards.first().description.contains("特定条件"))
    }

    @Test
    fun hero_summary_highlights_recent_unlock_and_next_near_completion() {
        val recent = achievementItem(
            key = "recent",
            name = "雨声留档",
            unlocked = true,
            unlockedAt = 8_000L
        )
        val near = achievementItem(
            key = "near",
            name = "百篇里程碑",
            progress = 92,
            target = 100,
            tier = AchievementTier.EPIC
        )
        val stats = AchievementStats(
            unlockedCount = 5,
            totalCount = 12,
            categoryCounts = emptyMap()
        )

        val summary = buildAchievementHeroSummary(
            stats = stats,
            items = listOf(recent, near)
        )

        assertTrue(summary.headline.contains("雨声留档"))
        assertTrue(summary.supportingLine.contains("百篇里程碑"))
        assertEquals(5f / 12f, summary.completionFraction, 0.0001f)
        assertEquals(1, summary.unlockedLegendaryCount)
    }

    private fun achievementItem(
        key: String,
        name: String = key,
        description: String = "desc-$key",
        category: AchievementCategory = AchievementCategory.WRITING,
        tier: AchievementTier = AchievementTier.LEGENDARY,
        hidden: Boolean = false,
        unlocked: Boolean = false,
        unlockedAt: Long? = null,
        progress: Int = if (unlocked) 1 else 0,
        target: Int = 1
    ): AchievementItem {
        return AchievementItem(
            def = AchievementDef(
                key = key,
                name = name,
                description = description,
                category = category,
                tier = tier,
                iconEmoji = "",
                flavorText = "",
                target = target,
                isHidden = hidden
            ),
            state = AchievementState(
                key = key,
                progress = progress,
                unlocked = unlocked,
                unlockedAt = unlockedAt,
                relatedEntryId = null
            )
        )
    }
}
