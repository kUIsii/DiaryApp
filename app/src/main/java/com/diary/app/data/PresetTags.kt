package com.diary.app.data

internal data class PresetTagSyncResult(
    val missingTags: List<Tag>,
    val duplicatePresetTags: List<Tag>
)

internal val defaultPresetTags = listOf(
    Tag(name = "生活", color = 0xFF667EEA, isPreset = true),
    Tag(name = "工作", color = 0xFFE74C3C, isPreset = true),
    Tag(name = "学习", color = 0xFF2ECC71, isPreset = true),
    Tag(name = "旅行", color = 0xFFE67E22, isPreset = true),
    Tag(name = "感悟", color = 0xFF9B59B6, isPreset = true),
    Tag(name = "健康", color = 0xFF1ABC9C, isPreset = true),
    Tag(name = "财务", color = 0xFFF1C40F, isPreset = true),
    Tag(name = "社交", color = 0xFFE91E63, isPreset = true)
)

internal fun syncPresetTags(existingTags: List<Tag>): PresetTagSyncResult {
    val byName = existingTags.groupBy { normalizeTagNameForMatching(it.name) }
    val missingTags = defaultPresetTags.filter { preset ->
        byName[normalizeTagNameForMatching(preset.name)].isNullOrEmpty()
    }

    val duplicatePresetTags = byName.values.flatMap { groupedTags ->
        val presetTags = groupedTags.filter { it.isPreset }.sortedBy { it.id }
        if (presetTags.size <= 1) emptyList() else presetTags.drop(1)
    }

    return PresetTagSyncResult(
        missingTags = missingTags,
        duplicatePresetTags = duplicatePresetTags
    )
}
