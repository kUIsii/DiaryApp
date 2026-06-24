package com.diary.app.data

/**
 * 预定义称号数据 — 35+个称号
 * 在数据库迁移时插入
 */
object TitleSeedData {

    val allTitles: List<TitleDefinition> = listOf(
        // ===== 时间旅人（7个）=====
        TitleDefinition(
            key = "night_poet",
            name = "凌晨诗人",
            description = "在凌晨0-3点写过3篇以上日记",
            category = "time",
            iconName = "NightsStay",
            tier = 2,
            flavorText = "月光是你的墨水，星星是你的标点。在大多数人沉睡的时候，你的笔尖在纸上跳舞。"
        ),
        TitleDefinition(
            key = "dawn_recorder",
            name = "黎明记录者",
            description = "在凌晨3-5点写过日记",
            category = "time",
            iconName = "DarkMode",
            tier = 2,
            flavorText = "你在黎明前醒来，记录下梦境与现实交界的那一刻。世界还很安静，只有你和你的文字。"
        ),
        TitleDefinition(
            key = "morning_writer",
            name = "晨光之笔",
            description = "在5-7点写过5篇以上日记",
            category = "time",
            iconName = "LightMode",
            tier = 2,
            flavorText = "当第一缕阳光穿过窗帘，你已经在记录新的一天。你是晨光的收藏者。"
        ),
        TitleDefinition(
            key = "afternoon_dreamer",
            name = "午后漫想家",
            description = "80%以上日记写于12-15点",
            category = "time",
            iconName = "WbSunny",
            tier = 1,
            flavorText = "午后是你最放松的时刻，阳光正好，思绪最自由。你在这个时间段找到了属于自己的节奏。"
        ),
        TitleDefinition(
            key = "night_owl",
            name = "夜猫子",
            description = "70%以上日记写于22-2点",
            category = "time",
            iconName = "NightsStay",
            tier = 1,
            flavorText = "夜晚是你的主场。当世界安静下来，你的文字开始苏醒。"
        ),
        TitleDefinition(
            key = "weekday_killer",
            name = "星期杀手",
            description = "在每一个星期几都写过日记",
            category = "time",
            iconName = "DateRange",
            tier = 2,
            flavorText = "你没有'写作日'的概念——每一天都是你的写作日。星期几对你来说只是日期的前缀。"
        ),
        TitleDefinition(
            key = "time_capsule_master",
            name = "时光胶囊",
            description = "连续12个月每月都写了日记",
            category = "time",
            iconName = "CalendarMonth",
            tier = 3,
            flavorText = "整整一年，你没有让任何一个月份空白。这不是坚持，这是一种生活方式。"
        ),

        // ===== 情绪画师（6个）=====
        TitleDefinition(
            key = "optimist",
            name = "乐观主义者",
            description = "连续10篇日记心情等级都在5以上",
            category = "mood",
            iconName = "SentimentVerySatisfied",
            tier = 2,
            flavorText = "在你的世界里，阳光总是比乌云多。你是一个天生的乐观主义者。"
        ),
        TitleDefinition(
            key = "deep_thinker",
            name = "深度思考者",
            description = "连续10篇日记心情等级都在2以下",
            category = "mood",
            iconName = "Psychology",
            tier = 2,
            flavorText = "你习惯在安静中思考，在思考中沉淀。低落不是坏事，它只是深度的另一种表现。"
        ),
        TitleDefinition(
            key = "mood_palette",
            name = "情绪调色板",
            description = "使用过全部6种心情等级",
            category = "mood",
            iconName = "Palette",
            tier = 1,
            flavorText = "你的情绪世界是丰富多彩的——从沮丧到兴奋，你体验了完整的情感光谱。你是自己情绪的调色师。"
        ),
        TitleDefinition(
            key = "mood_rollercoaster",
            name = "心情过山车",
            description = "单月内心情从最高到最低的变化超过4次",
            category = "mood",
            iconName = "TrendingUp",
            tier = 1,
            flavorText = "你的情绪像过山车一样起伏不定。这不是不稳定，这是丰富。每一次波动都是你在认真感受生活。"
        ),
        TitleDefinition(
            key = "calm_sea",
            name = "平静之海",
            description = "连续20篇日记心情稳定在3-4之间",
            category = "mood",
            iconName = "Water",
            tier = 2,
            flavorText = "你的心境像一片平静的海面，没有太大的波澜，但有着深沉的力量。平和是一种难得的智慧。"
        ),
        TitleDefinition(
            key = "mood_weather_link",
            name = "情绪日记家",
            description = "心情变化与天气有明显相关性",
            category = "mood",
            iconName = "Insights",
            tier = 2,
            flavorText = "你与自然有着微妙的共鸣——天气的变化似乎能直接触动你的情绪。你是一个敏感而细腻的人。"
        ),

        // ===== 风雨行者（6个）=====
        TitleDefinition(
            key = "rain_collector",
            name = "雨天收藏家",
            description = "在雨天写了20篇以上日记",
            category = "weather",
            iconName = "WaterDrop",
            tier = 2,
            flavorText = "雨天对很多人来说是阴郁的，但对你来说是灵感的来源。你收藏了20个下雨天的故事。"
        ),
        TitleDefinition(
            key = "snow_writer",
            name = "雪日笔记",
            description = "在下雪天写过日记",
            category = "weather",
            iconName = "AcUnit",
            tier = 1,
            flavorText = "当雪花飘落的时候，你拿起了笔。这一天值得被记住。"
        ),
        TitleDefinition(
            key = "storm_writer",
            name = "风暴作家",
            description = "在大风天写了5篇以上日记",
            category = "weather",
            iconName = "Storm",
            tier = 2,
            flavorText = "风在窗外呼啸，你的笔尖在纸上疾书。风暴给你带来了力量。"
        ),
        TitleDefinition(
            key = "sunny_recorder",
            name = "晴天记录者",
            description = "在晴天写了50篇以上日记",
            category = "weather",
            iconName = "WbSunny",
            tier = 3,
            flavorText = "阳光明媚的日子里，你的记录从未间断。你是晴天的忠实记录者。"
        ),
        TitleDefinition(
            key = "all_weather",
            name = "万事皆记",
            description = "在所有天气类型下都写过日记",
            category = "weather",
            iconName = "CloudDone",
            tier = 2,
            flavorText = "无论外面是晴是雨，是风是雪，你都在记录。天气无法阻止你的笔尖。"
        ),
        TitleDefinition(
            key = "fearless_recorder",
            name = "无惧风雨",
            description = "在极端天气下都写了日记",
            category = "weather",
            iconName = "SevereWeather",
            tier = 3,
            flavorText = "普通人在暴风雨中会停下脚步，但你不会。你记录了最狂野的天气。"
        ),

        // ===== 文字匠人（6个）=====
        TitleDefinition(
            key = "thousand_words",
            name = "千字长文",
            description = "单篇日记超过1000字",
            category = "writing",
            iconName = "TextFields",
            tier = 1,
            flavorText = "1000字，对你来说只是一个开始。你有把一件事说清楚的天赋。"
        ),
        TitleDefinition(
            key = "brief_master",
            name = "微言大义",
            description = "单篇日记少于50字但被收藏了",
            category = "writing",
            iconName = "ShortText",
            tier = 1,
            flavorText = "有时候，50个字就够了。你是一个懂得留白的人。"
        ),
        TitleDefinition(
            key = "photo_diary",
            name = "图文大师",
            description = "单篇日记包含3张以上图片",
            category = "writing",
            iconName = "Collections",
            tier = 1,
            flavorText = "你的日记不是文字的独白，而是图文的交响。每一张图片都承载着文字无法表达的情感。"
        ),
        TitleDefinition(
            key = "tag_master",
            name = "标签达人",
            description = "创建并使用了10个以上不同的标签",
            category = "writing",
            iconName = "Label",
            tier = 1,
            flavorText = "你是一个有条理的人——用10个以上的标签为生活分类。你的日记是一座井井有条的图书馆。"
        ),
        TitleDefinition(
            key = "collector",
            name = "收藏鉴赏家",
            description = "收藏了20篇以上自己的日记",
            category = "writing",
            iconName = "Bookmark",
            tier = 2,
            flavorText = "你收藏了20篇属于自己的故事。你懂得欣赏自己的文字，这是一种难得的自我肯定。"
        ),
        TitleDefinition(
            key = "fifty_thousand_words",
            name = "万字耕耘者",
            description = "累计写作超过5万字",
            category = "writing",
            iconName = "TextSnippet",
            tier = 3,
            flavorText = "5万字，相当于一本短篇小说的长度。你已经写下了属于自己的故事。"
        ),

        // ===== 习惯先锋（5个）=====
        TitleDefinition(
            key = "daily_writer",
            name = "日更达人",
            description = "连续30天每天写日记",
            category = "habit",
            iconName = "Whatshot",
            tier = 2,
            flavorText = "30天，一天不落。你已经把写日记变成了像呼吸一样自然的事。"
        ),
        TitleDefinition(
            key = "hundred_days",
            name = "百日坚持",
            description = "连续100天每天写日记",
            category = "habit",
            iconName = "MilitaryTech",
            tier = 3,
            flavorText = "100天，你证明了坚持不是口号，而是行动。这个小岛因为你而繁荣。"
        ),
        TitleDefinition(
            key = "returnee",
            name = "回归者",
            description = "断写超过30天后重新开始写日记",
            category = "habit",
            iconName = "Replay",
            tier = 1,
            flavorText = "你离开了一段时间，但你回来了。回归比从未离开更需要勇气。"
        ),
        TitleDefinition(
            key = "flash_writer",
            name = "闪电记录",
            description = "在1分钟内完成一篇日记",
            category = "habit",
            iconName = "FlashOn",
            tier = 1,
            flavorText = "1分钟，你捕捉了一个瞬间。有时候，最真实的情感只需要一句话来记录。"
        ),
        TitleDefinition(
            key = "deep_writer",
            name = "深度笔耕",
            description = "单次写作超过30分钟",
            category = "habit",
            iconName = "Timelapse",
            tier = 1,
            flavorText = "30分钟，你沉浸在自己的世界里。这是一种难得的专注力。"
        ),
        TitleDefinition(
            key = "twin_stars",
            name = "双子星",
            description = "同一天写了2篇以上日记",
            category = "habit",
            iconName = "TwoMp",
            tier = 1,
            flavorText = "一天两篇，你是一个多产的记录者。也许这一天太丰富了，一篇日记装不下。"
        ),

        // ===== 隐藏彩蛋（5个）=====
        TitleDefinition(
            key = "time_traveler",
            name = "时间旅行者",
            description = "",
            category = "hidden",
            iconName = "Celebration",
            tier = 2,
            isHidden = true,
            flavorText = "新的一年第一天，你在记录。你是一个有仪式感的人。"
        ),
        TitleDefinition(
            key = "new_year_eve",
            name = "跨年守夜人",
            description = "",
            category = "hidden",
            iconName = "NewReleases",
            tier = 2,
            isHidden = true,
            flavorText = "旧年的最后一小时，你在和过去告别。新年快乐。"
        ),
        TitleDefinition(
            key = "midnight_bell",
            name = "午夜钟声",
            description = "",
            category = "hidden",
            iconName = "Schedule",
            tier = 3,
            isHidden = true,
            flavorText = "午夜的钟声敲响，你还在写。你捕捉了新旧交替的那一刻。"
        ),
        TitleDefinition(
            key = "full_moon",
            name = "满月记录",
            description = "",
            category = "hidden",
            iconName = "NightsStay",
            tier = 2,
            isHidden = true,
            flavorText = "满月之夜，你在记录。月光似乎给了你灵感。"
        ),
        TitleDefinition(
            key = "first_echo",
            name = "首篇回响",
            description = "",
            category = "hidden",
            iconName = "AutoFixHigh",
            tier = 1,
            isHidden = true,
            flavorText = "你回过头去，给第一篇日记加上了注脚。你是一个念旧的人。"
        )
    )
}
