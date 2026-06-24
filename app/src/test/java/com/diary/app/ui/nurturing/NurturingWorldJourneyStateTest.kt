package com.diary.app.ui.nurturing

import com.diary.app.data.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NurturingWorldJourneyStateTest {

    @Test
    fun prioritizes_pet_comfort_when_pet_is_worried() {
        val state = buildNurturingJourneyState(
            petState = PetState.WORRIED,
            islandLevel = 6,
            recentAchievementUnlock = null,
            hasRareDiscovery = false,
            nearMilestoneName = "七日守望",
            streakDays = 3
        )

        assertEquals(NurturingRouteTarget.PET, state.primaryTarget)
        assertEquals("先安抚陪伴精灵", state.steps.first().title)
        assertEquals(NurturingRouteTarget.ISLAND, state.steps[1].target)
        assertTrue(state.summary.contains("七日守望"))
    }

    @Test
    fun prioritizes_showcasing_recent_unlock_when_new_collection_arrives() {
        val state = buildNurturingJourneyState(
            petState = PetState.EXCITED,
            islandLevel = 12,
            recentAchievementUnlock = "月航收藏",
            hasRareDiscovery = true,
            nearMilestoneName = "海风观察者",
            streakDays = 9
        )

        assertEquals(NurturingRouteTarget.ACHIEVEMENT, state.primaryTarget)
        assertEquals("把新珍藏摆进展柜", state.steps.first().title)
        assertEquals(NurturingRouteTarget.PET, state.steps[1].target)
        assertTrue(state.headline.contains("月航收藏"))
    }
}
