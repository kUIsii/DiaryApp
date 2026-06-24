package com.diary.app.ui.nurturing

import androidx.annotation.DrawableRes
import com.diary.app.R

@DrawableRes
fun petArtRes(key: PetArtKey): Int = when (key) {
    PetArtKey.CALM -> R.drawable.nurturing_pet_calm
    PetArtKey.WORRIED -> R.drawable.nurturing_pet_worried
    PetArtKey.CELEBRATION -> R.drawable.nurturing_pet_celebration
}

@DrawableRes
fun islandArtRes(key: IslandArtKey): Int = when (key) {
    IslandArtKey.TREEHOUSE -> R.drawable.nurturing_island_treehouse
    IslandArtKey.SECRET_GLOW -> R.drawable.nurturing_island_moonpond
}

@DrawableRes
fun achievementArtRes(key: AchievementArtKey): Int = when (key) {
    AchievementArtKey.RARE_GALLERY -> R.drawable.nurturing_badge_rare
    AchievementArtKey.LEGENDARY_SHOWCASE -> R.drawable.nurturing_badge_legendary
}
