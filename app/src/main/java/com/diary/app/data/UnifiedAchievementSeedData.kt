package com.diary.app.data

/**
 * Unified achievement definitions.
 * All achievements are defined here with consistent category, tier and copy.
 */
object UnifiedAchievementSeedData {

    val allAchievements: List<AchievementDef> = listOf(

        // ═══════════════════════════════════════════════════════
        //  文字匠人 (WRITING) — from old Achievement system
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "first_entry",
            name = "初出茅庐",
            description = "写下第一篇日记",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDF1F",
            flavorText = "每一段旅程都有第一步，你的第一步从这里开始。",
            target = 1
        ),
        AchievementDef(
            key = "entries_10",
            name = "笔耕不辍",
            description = "累计写下 10 篇日记",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDCDD",
            flavorText = "10篇日记，你已经养成了记录的习惯。文字是你最好的朋友。",
            target = 10
        ),
        AchievementDef(
            key = "entries_50",
            name = "日记达人",
            description = "累计写下 50 篇日记",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83D\uDCDA",
            flavorText = "50篇日记，你的文字已经可以编成一本小书了。坚持就是力量。",
            target = 50
        ),
        AchievementDef(
            key = "entries_100",
            name = "百篇里程碑",
            description = "累计写下 100 篇日记",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.EPIC,
            iconEmoji = "\uD83C\uDFC6",
            flavorText = "100篇日记，你已经是一位真正的记录者。文字见证了你的成长。",
            target = 100
        ),
        AchievementDef(
            key = "words_10000",
            name = "万字作者",
            description = "累计写作 10,000 字",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDCAC",
            flavorText = "一万字，相当于一篇短篇小说的长度。你的笔下有一个世界。",
            target = 10000
        ),
        AchievementDef(
            key = "words_100000",
            name = "十万字巨匠",
            description = "累计写作 100,000 字",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.EPIC,
            iconEmoji = "\uD83D\uDCD6",
            flavorText = "十万字，相当于一本中篇小说。你已经写下了一部属于自己的作品。",
            target = 100000
        ),
        AchievementDef(
            key = "tags_5",
            name = "标签达人",
            description = "创建 5 个标签",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDFF7\uFE0F",
            flavorText = "你是一个有条理的人——用标签为生活分类。",
            target = 5
        ),
        AchievementDef(
            key = "images_10",
            name = "图文并茂",
            description = "在日记中添加 10 张图片",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDDBC\uFE0F",
            flavorText = "你的日记不是文字的独白，而是图文的交响。",
            target = 10
        ),

        // ═══════════════════════════════════════════════════════
        //  习惯先锋 (HABIT) — streak achievements
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "streak_7",
            name = "一周坚持",
            description = "连续 7 天写日记",
            category = AchievementCategory.HABIT,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDCC6",
            flavorText = "7天，你证明了坚持不是口号，而是行动。",
            target = 7
        ),
        AchievementDef(
            key = "streak_30",
            name = "月度坚持",
            description = "连续 30 天写日记",
            category = AchievementCategory.HABIT,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83D\uDD25",
            flavorText = "30天，你已经把写日记变成了像呼吸一样自然的事。",
            target = 30
        ),
        AchievementDef(
            key = "daily_writer",
            name = "日更达人",
            description = "连续 30 天每天写日记",
            category = AchievementCategory.HABIT,
            tier = AchievementTier.RARE,
            iconEmoji = "\u26A1",
            flavorText = "30天，一天不落。你已经把写日记变成了生活的一部分。",
            target = 30
        ),
        AchievementDef(
            key = "hundred_days",
            name = "百日坚持",
            description = "连续 100 天每天写日记",
            category = AchievementCategory.HABIT,
            tier = AchievementTier.LEGENDARY,
            iconEmoji = "\uD83C\uDFC5",
            flavorText = "100天，你把日常写成了一条稳定延伸的生活轨迹。这不是冲刺，而是长期留下的痕迹。",
            target = 100
        ),

        // ═══════════════════════════════════════════════════════
        //  时间旅人 (TIME)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "night_writer",
            name = "夜猫子",
            description = "在凌晨 0-5 点写日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDF19",
            flavorText = "夜晚是你的主场。当世界安静下来，你的文字开始苏醒。",
            target = 1
        ),
        AchievementDef(
            key = "early_bird",
            name = "早起鸟",
            description = "在早上 5-7 点写日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDF05",
            flavorText = "当第一缕阳光穿过窗帘，你已经在记录新的一天。你是晨光的收藏者。",
            target = 1
        ),
        AchievementDef(
            key = "night_poet",
            name = "凌晨诗人",
            description = "在凌晨0-3点写过3篇以上日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.RARE,
            iconEmoji = "\u2B50",
            flavorText = "月光是你的墨水，星星是你的标点。在大多数人沉睡的时候，你的笔尖在纸上跳舞。",
            target = 3
        ),
        AchievementDef(
            key = "dawn_recorder",
            name = "黎明记录者",
            description = "在凌晨3-5点写过日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83C\uDF06",
            flavorText = "你在黎明前醒来，记录下梦境与现实交界的那一刻。世界还很安静，只有你和你的文字。",
            target = 1
        ),
        AchievementDef(
            key = "morning_writer",
            name = "晨光之笔",
            description = "在5-7点写过5篇以上日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.RARE,
            iconEmoji = "\u2728",
            flavorText = "当第一缕阳光穿过窗帘，你已经在记录新的一天。你是晨光的收藏者。",
            target = 5
        ),
        AchievementDef(
            key = "weekday_killer",
            name = "星期杀手",
            description = "在每一个星期几都写过日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83D\uDCC5",
            flavorText = "你没有'写作日'的概念——每一天都是你的写作日。星期几对你来说只是日期的前缀。",
            target = 7
        ),
        AchievementDef(
            key = "time_capsule_master",
            name = "时光胶囊",
            description = "连续12个月每月都写了日记",
            category = AchievementCategory.TIME,
            tier = AchievementTier.LEGENDARY,
            iconEmoji = "\u23F3",
            flavorText = "整整一年，你没有让任何一个月份空白。这不是坚持，这是一种生活方式。",
            target = 12
        ),

        // ═══════════════════════════════════════════════════════
        //  情绪画师 (MOOD)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "moods_5",
            name = "情绪丰富",
            description = "使用 5 种不同心情",
            category = AchievementCategory.MOOD,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDFA8",
            flavorText = "你的情绪世界是丰富多彩的——你体验了完整的情感光谱。",
            target = 5
        ),
        AchievementDef(
            key = "mood_palette",
            name = "情绪调色板",
            description = "使用过全部6种心情等级",
            category = AchievementCategory.MOOD,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDFA8",
            flavorText = "你的情绪世界是丰富多彩的——从沮丧到兴奋，你体验了完整的情感光谱。你是自己情绪的调色师。",
            target = 6
        ),
        AchievementDef(
            key = "optimist",
            name = "乐观主义者",
            description = "连续10篇日记心情等级都在5以上",
            category = AchievementCategory.MOOD,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83D\uDE0A",
            flavorText = "在你的世界里，阳光总是比乌云多。你是一个天生的乐观主义者。",
            target = 10
        ),
        AchievementDef(
            key = "deep_thinker",
            name = "深度思考者",
            description = "连续10篇日记心情等级都在2以下",
            category = AchievementCategory.MOOD,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83E\uDDE0",
            flavorText = "你习惯在安静中思考，在思考中沉淀。低落不是坏事，它只是深度的另一种表现。",
            target = 10
        ),
        AchievementDef(
            key = "calm_sea",
            name = "平静之海",
            description = "连续20篇日记心情稳定在3-4之间",
            category = AchievementCategory.MOOD,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83C\uDF0A",
            flavorText = "你的心境像一片平静的海面，没有太大的波澜，但有着深沉的力量。平和是一种难得的智慧。",
            target = 20
        ),
        AchievementDef(
            key = "mood_rollercoaster",
            name = "心情过山车",
            description = "单月内心情从最高到最低的变化超过4次",
            category = AchievementCategory.MOOD,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDFA2",
            flavorText = "你的情绪像过山车一样起伏不定。这不是不稳定，这是丰富。每一次波动都是你在认真感受生活。",
            target = 4
        ),

        // ═══════════════════════════════════════════════════════
        //  风雨行者 (WEATHER)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "all_weather",
            name = "风雨无阻",
            description = "在所有天气类型下写过日记",
            category = AchievementCategory.WEATHER,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83C\uDF29\uFE0F",
            flavorText = "无论外面是晴是雨，是风是雪，你都在记录。天气无法阻止你的笔尖。",
            target = 6
        ),
        AchievementDef(
            key = "rain_collector",
            name = "雨天收藏家",
            description = "在雨天写了20篇以上日记",
            category = AchievementCategory.WEATHER,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83D\uDCA7",
            flavorText = "雨天对很多人来说是阴郁的，但对你来说是灵感的来源。你收藏了20个下雨天的故事。",
            target = 20
        ),
        AchievementDef(
            key = "snow_writer",
            name = "雪日笔记",
            description = "在下雪天写过日记",
            category = AchievementCategory.WEATHER,
            tier = AchievementTier.COMMON,
            iconEmoji = "\u2744\uFE0F",
            flavorText = "当雪花飘落的时候，你拿起了笔。这一天值得被记住。",
            target = 1
        ),
        AchievementDef(
            key = "storm_writer",
            name = "风暴作家",
            description = "在大风天写了5篇以上日记",
            category = AchievementCategory.WEATHER,
            tier = AchievementTier.RARE,
            iconEmoji = "\u26A8\uFE0F",
            flavorText = "风在窗外呼啸，你的笔尖在纸上疾书。风暴给你带来了力量。",
            target = 5
        ),
        AchievementDef(
            key = "sunny_recorder",
            name = "晴天记录者",
            description = "在晴天写了50篇以上日记",
            category = AchievementCategory.WEATHER,
            tier = AchievementTier.EPIC,
            iconEmoji = "\u2600\uFE0F",
            flavorText = "阳光明媚的日子里，你的记录从未间断。你是晴天的忠实记录者。",
            target = 50
        ),
        AchievementDef(
            key = "fearless_recorder",
            name = "无惧风雨",
            description = "在极端天气下都写了日记",
            category = AchievementCategory.WEATHER,
            tier = AchievementTier.EPIC,
            iconEmoji = "\u26C8\uFE0F",
            flavorText = "普通人在暴风雨中会停下脚步，但你不会。你记录了最狂野的天气，也记录了最真实的自己。",
            target = 1
        ),

        // ═══════════════════════════════════════════════════════
        //  文字匠人 (WRITING) — extended collection milestones
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "thousand_words",
            name = "千字长文",
            description = "单篇日记超过1000字",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDCDD",
            flavorText = "1000字，对你来说只是一个开始。你有把一件事说清楚的天赋。",
            target = 1
        ),
        AchievementDef(
            key = "brief_master",
            name = "微言大义",
            description = "单篇日记少于50字但被收藏了",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\u2709\uFE0F",
            flavorText = "有时候，50个字就够了。你是一个懂得留白的人。",
            target = 1
        ),
        AchievementDef(
            key = "photo_diary",
            name = "图文大师",
            description = "单篇日记包含3张以上图片",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDDBC\uFE0F",
            flavorText = "你的日记不是文字的独白，而是图文的交响。每一张图片都承载着文字无法表达的情感。",
            target = 1
        ),
        AchievementDef(
            key = "collector",
            name = "收藏鉴赏家",
            description = "收藏了20篇以上自己的日记",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83D\uDCDA",
            flavorText = "你收藏了20篇属于自己的故事。你懂得欣赏自己的文字，这是一种难得的自我肯定。",
            target = 20
        ),
        AchievementDef(
            key = "fifty_thousand_words",
            name = "万字耕耘者",
            description = "累计写作超过5万字",
            category = AchievementCategory.WRITING,
            tier = AchievementTier.EPIC,
            iconEmoji = "\uD83D\uDCD6",
            flavorText = "5万字，相当于一本短篇小说的长度。你已经写下了属于自己的故事。这不是数字，这是你生命的重量。",
            target = 50000
        ),

        // ═══════════════════════════════════════════════════════
        //  收藏家 (COLLECTOR)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "favorite_1",
            name = "收藏家",
            description = "收藏第一篇日记",
            category = AchievementCategory.COLLECTOR,
            tier = AchievementTier.COMMON,
            iconEmoji = "\u2764\uFE0F",
            flavorText = "你收藏了第一篇属于自己的故事。",
            target = 1
        ),
        AchievementDef(
            key = "favorites_10",
            name = "珍藏满满",
            description = "收藏 10 篇日记",
            category = AchievementCategory.COLLECTOR,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDC96",
            flavorText = "10篇珍藏，每一篇都值得回味。你是一个懂得珍惜的人。",
            target = 10
        ),

        // ═══════════════════════════════════════════════════════
        //  探险家 (EXPLORER)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "returnee",
            name = "回归者",
            description = "断写超过30天后重新开始写日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDD04",
            flavorText = "你离开了一段时间，但你回来了。回归比从未离开更需要勇气。",
            target = 1
        ),
        AchievementDef(
            key = "flash_writer",
            name = "闪电记录",
            description = "在1分钟内完成一篇日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.COMMON,
            iconEmoji = "\u26A1",
            flavorText = "1分钟，你捕捉了一个瞬间。有时候，最真实的情感只需要一句话来记录。",
            target = 1
        ),
        AchievementDef(
            key = "deep_writer",
            name = "深度笔耕",
            description = "单次写作超过30分钟",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.COMMON,
            iconEmoji = "\u23F1\uFE0F",
            flavorText = "30分钟，你沉浸在自己的世界里。这是一种难得的专注力。",
            target = 1
        ),
        AchievementDef(
            key = "twin_stars",
            name = "双子星",
            description = "同一天写了2篇以上日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83C\uDF1F",
            flavorText = "一天两篇，你是一个多产的记录者。也许这一天太丰富了，一篇日记装不下。",
            target = 1
        ),

        // ═══════════════════════════════════════════════════════
        //  隐藏彩蛋 (EXPLORER with isHidden)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "time_traveler",
            name = "时间旅行者",
            description = "在新年第一天写日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83C\uDF89",
            flavorText = "新的一年第一天，你在记录。你是一个有仪式感的人。",
            isHidden = true
        ),
        AchievementDef(
            key = "new_year_eve",
            name = "跨年守夜人",
            description = "在旧年最后一天写日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83C\uDF87",
            flavorText = "旧年的最后一小时，你在和过去告别。新年快乐。",
            isHidden = true
        ),
        AchievementDef(
            key = "midnight_bell",
            name = "午夜钟声",
            description = "在午夜零点整写日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.EPIC,
            iconEmoji = "\uD83D\uDD14",
            flavorText = "午夜的钟声敲响，你还在写。你捕捉了新旧交替的那一刻。",
            isHidden = true
        ),
        AchievementDef(
            key = "full_moon",
            name = "满月记录",
            description = "在满月之夜写日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.RARE,
            iconEmoji = "\uD83C\uDF15",
            flavorText = "满月之夜，你在记录。月光似乎给了你灵感。",
            isHidden = true
        ),
        AchievementDef(
            key = "first_echo",
            name = "首篇回响",
            description = "回顾并编辑第一篇日记",
            category = AchievementCategory.EXPLORER,
            tier = AchievementTier.COMMON,
            iconEmoji = "\uD83D\uDD01",
            flavorText = "你回过头去，给第一篇日记加上了注脚。你是一个念旧的人。",
            isHidden = true
        ),

        // ═══════════════════════════════════════════════════════
        //  传说收藏 (LEGENDARY)
        // ═══════════════════════════════════════════════════════

        AchievementDef(
            key = "legendary_entries_500",
            name = "五百家书",
            description = "累计写下 500 篇日记",
            category = AchievementCategory.LEGENDARY,
            tier = AchievementTier.LEGENDARY,
            iconEmoji = "\uD83D\uDCDC",
            flavorText = "500篇日记，你的文字已经可以装满一个图书馆。你是真正的记录大师。",
            target = 500
        ),
        AchievementDef(
            key = "legendary_streak_365",
            name = "一整年",
            description = "连续 365 天写日记",
            category = AchievementCategory.LEGENDARY,
            tier = AchievementTier.LEGENDARY,
            iconEmoji = "\uD83C\uDF1F",
            flavorText = "365天，你把一年完整地写进了收藏册。每一次落笔都不是重复，而是对生活的持续回应。",
            target = 365
        ),
        AchievementDef(
            key = "legendary_words_million",
            name = "百万字大师",
            description = "累计写作 1,000,000 字",
            category = AchievementCategory.LEGENDARY,
            tier = AchievementTier.LEGENDARY,
            iconEmoji = "\uD83C\uDFC6",
            flavorText = "一百万字，相当于一本长篇小说。你已经写下了一部史诗。这不是数字，这是你生命的重量。",
            target = 1000000
        ),
        AchievementDef(
            key = "legendary_all_categories",
            name = "全能记录者",
            description = "解锁所有分类的成就",
            category = AchievementCategory.LEGENDARY,
            tier = AchievementTier.LEGENDARY,
            iconEmoji = "\uD83C\uDFC5",
            flavorText = "你在每个领域都留下了足迹。你是真正的全能记录者，一个完整的灵魂。",
            target = 8
        )
    )

    /**
     * Get achievement definitions as a map for quick lookup by key.
     */
    val byKey: Map<String, AchievementDef> = allAchievements.associateBy { it.key }

    /**
     * Get achievements grouped by category.
     */
    val byCategory: Map<AchievementCategory, List<AchievementDef>> =
        allAchievements.groupBy { it.category }
}
