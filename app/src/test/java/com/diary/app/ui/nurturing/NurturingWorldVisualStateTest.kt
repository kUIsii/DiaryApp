package com.diary.app.ui.nurturing

import com.diary.app.data.AchievementTier
import com.diary.app.data.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NurturingWorldVisualStateTest {

    @Test
    fun uses_celebration_pet_art_when_recent_unlock_pushes_positive_state() {
        val state = buildPetSceneVisualState(
            petState = PetState.EXCITED,
            recentAchievementUnlock = "月光守望者",
            islandLevel = 9
        )

        assertEquals(PetArtKey.CELEBRATION, state.artKey)
        assertEquals("刚刚替你庆祝过新的珍藏", state.sceneLabel)
        assertTrue(state.companionHint.contains("月光守望者"))
    }

    @Test
    fun uses_secret_island_art_and_guidance_when_rare_event_is_active() {
        val state = buildIslandVisualState(
            islandLevel = 12,
            petState = PetState.CALM,
            hasRareDiscovery = true,
            activeBuffCount = 1,
            activeAnimalsCount = 4,
            recentAchievementUnlock = "夜航收藏"
        )

        assertEquals(IslandArtKey.SECRET_GLOW, state.artKey)
        assertEquals("稀有现象正在把夜色往更深处牵引", state.headline)
        assertTrue(state.guidance.contains("夜航收藏"))
    }

    @Test
    fun upgrades_achievement_hero_and_next_action_for_legendary_progress() {
        val state = buildAchievementVisualState(
            unlockedCount = 18,
            totalCount = 24,
            legendaryUnlockedCount = 2,
            nearMilestoneName = "七日守望",
            petState = PetState.HAPPY,
            islandLevel = 14
        )

        assertEquals(AchievementArtKey.LEGENDARY_SHOWCASE, state.artKey)
        assertEquals("你的收藏馆已经有了可以镇场的传说展品", state.heroLine)
        assertEquals(AchievementTier.LEGENDARY, state.emphasisTier)
        assertTrue(state.nextActionHint.contains("七日守望"))
    }
}
