package com.diary.app.data

/**
 * 反馈文案生成器
 * 根据触发类型和宠物性格生成反馈文案
 */
object FeedbackGenerator {

    /**
     * 根据触发类型和风格生成反馈
     */
    fun generate(
        trigger: FeedbackTrigger,
        style: FeedbackStyle,
        petName: String = "小记"
    ): String {
        val templates = feedbackTemplates[trigger] ?: return ""
        val styleTemplates = templates[style] ?: templates[FeedbackStyle.CALM] ?: return ""
        return styleTemplates.random().replace("{name}", petName)
    }

    /**
     * 反馈文案模板
     * 按触发类型和风格分类
     */
    private val feedbackTemplates: Map<FeedbackTrigger, Map<FeedbackStyle, List<String>>> = mapOf(
        // 首次写日记
        FeedbackTrigger.FIRST_ENTRY to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "哇！{name}超级开心！你终于来啦！",
                "嗨嗨！{name}等你好久了！一起开始吧！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "咦？{name}发现了一个新朋友！你好呀~",
                "哦？{name}很好奇你会写下什么呢~"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "欢迎！{name}相信你会写出很棒的内容。",
                "第一步总是最重要的。{name}陪你一起~"
            ),
            FeedbackStyle.WARM to listOf(
                "终于等到你了。{name}会一直在这里。",
                "你好呀~{name}很高兴认识你。"
            ),
            FeedbackStyle.CALM to listOf(
                "你好。{name}在这里。",
                "欢迎。{name}会陪着你。"
            )
        ),

        // 连续记录3天
        FeedbackTrigger.STREAK_3_DAYS to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "三天啦！{name}要给你鼓掌！啪啪啪！",
                "连续三天！{name}觉得你超棒的！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "三天了呢~{name}发现你越来越会记录了。",
                "嗯？连续三天？{name}有点佩服你了。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "三天连续记录，{name}看到了你的坚持。",
                "很棒的开始！{name}相信你会继续下去。"
            ),
            FeedbackStyle.WARM to listOf(
                "三天了。{name}一直在这里陪着你。",
                "你的坚持让{name}很感动。"
            ),
            FeedbackStyle.CALM to listOf(
                "三天了。不错的开始。",
                "{name}注意到你的坚持了。"
            )
        ),

        // 积极内容
        FeedbackTrigger.POSITIVE_CONTENT to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "哇！{name}感受到你的开心了！",
                "好棒！{name}也想跟你一起开心！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "嗯~{name}感觉到你在笑呢。",
                "开心的事情？{name}也想听听~"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "看到你开心，{name}也很高兴。",
                "美好的时刻值得被记录。"
            ),
            FeedbackStyle.WARM to listOf(
                "你的快乐感染了{name}。",
                "真好。{name}为你开心。"
            ),
            FeedbackStyle.CALM to listOf(
                "嗯，开心就好。",
                "{name}感受到你的喜悦。"
            )
        ),

        // 消极内容
        FeedbackTrigger.NEGATIVE_CONTENT to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "别难过啦！{name}给你一个大大的拥抱！",
                "{name}在这里！不开心的事情会过去的！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "嗯...{name}感觉到你不太开心。",
                "发生什么了？{name}想陪你聊聊。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "没关系的。{name}相信你能度过难关。",
                "低落是暂时的。{name}陪你一起面对。"
            ),
            FeedbackStyle.WARM to listOf(
                "过来。{name}给你一个温暖的拥抱。",
                "不开心的时候，{name}会一直在这里。"
            ),
            FeedbackStyle.CALM to listOf(
                "嗯。{name}在这里。",
                "不着急。慢慢来。"
            )
        ),

        // 深夜记录
        FeedbackTrigger.LATE_NIGHT to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "这么晚还没睡呀？{name}也陪你！",
                "夜猫子！{name}给你点个赞！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "深夜了呢~{name}好奇你在想什么。",
                "月亮出来了。{name}陪你一起记录。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "深夜的记录格外珍贵。{name}理解你。",
                "把今天的事情记录下来，明天会更好。"
            ),
            FeedbackStyle.WARM to listOf(
                "夜深了。{name}轻声陪你。",
                "晚安前的记录。{name}在这里。"
            ),
            FeedbackStyle.CALM to listOf(
                "夜深了。早点休息。",
                "{name}在这里。"
            )
        ),

        // 长期未记录
        FeedbackTrigger.LONG_ABSENCE to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "你终于来啦！{name}想死你了！",
                "好久不见！{name}等你好久了！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "咦？{name}好久没见到你了。",
                "你去哪了？{name}有点想你。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "回来就好。{name}一直在这里等你。",
                "没关系，什么时候回来都不晚。"
            ),
            FeedbackStyle.WARM to listOf(
                "你回来了。{name}一直在等你。",
                "好久不见。{name}很想你。"
            ),
            FeedbackStyle.CALM to listOf(
                "你回来了。",
                "嗯。{name}在这里。"
            )
        ),

        // 压力内容
        FeedbackTrigger.STRESS_CONTENT to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "深呼吸！{name}陪你一起面对！",
                "没关系的！{name}相信你能搞定！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "嗯...{name}感觉到你压力有点大。",
                "压力大的时候，写下来会好一点。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "压力是成长的一部分。{name}相信你。",
                "一步步来。{name}陪你。"
            ),
            FeedbackStyle.WARM to listOf(
                "累了就休息一下。{name}在这里。",
                "不着急。{name}陪你慢慢来。"
            ),
            FeedbackStyle.CALM to listOf(
                "深呼吸。一切都会好的。",
                "{name}在这里。"
            )
        ),

        // 目标达成
        FeedbackTrigger.GOAL_ACHIEVED to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "太棒了！{name}要给你放烟花！",
                "成功啦！{name}超级为你骄傲！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "哇~{name}发现你做到了！",
                "厉害！{name}想看看你是怎么做到的。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "你做到了！{name}就知道你可以。",
                "很棒！{name}为你的努力点赞。"
            ),
            FeedbackStyle.WARM to listOf(
                "你的努力没有白费。{name}为你高兴。",
                "真好。{name}一直看着你进步。"
            ),
            FeedbackStyle.CALM to listOf(
                "嗯，做得不错。",
                "{name}看到了你的进步。"
            )
        ),

        // 新话题
        FeedbackTrigger.NEW_TOPIC to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "新话题！{name}好兴奋！",
                "哦？{name}对这个超感兴趣的！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "咦？新话题？{name}很好奇！",
                "哦？{name}想多了解一下。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "新的话题，新的探索。{name}支持你。",
                "尝试新东西总是好的。"
            ),
            FeedbackStyle.WARM to listOf(
                "新的记录。{name}陪你一起。",
                "每一次记录都是新的开始。"
            ),
            FeedbackStyle.CALM to listOf(
                "嗯，新的话题。",
                "{name}在这里。"
            )
        ),

        // 每日问候
        FeedbackTrigger.DAILY_GREETING to mapOf(
            FeedbackStyle.LIVELY to listOf(
                "早上好！{name}精神满满！",
                "新的一天！{name}陪你一起！"
            ),
            FeedbackStyle.CURIOUS to listOf(
                "今天会有什么有趣的事呢？{name}期待~",
                "新的一天。{name}好奇你会记录什么。"
            ),
            FeedbackStyle.ENCOURAGING to listOf(
                "今天也要加油哦！{name}相信你。",
                "新的一天，新的开始。"
            ),
            FeedbackStyle.WARM to listOf(
                "早安。{name}在这里。",
                "新的一天。{name}陪着你。"
            ),
            FeedbackStyle.CALM to listOf(
                "早安。",
                "嗯。新的一天。"
            )
        )
    )
}
