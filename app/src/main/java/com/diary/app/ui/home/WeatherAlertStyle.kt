package com.diary.app.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.diary.app.ui.theme.LocalExtendedColors

/**
 * 预警等级对应的主题色。
 *
 * 与 [com.diary.app.ui.home.WeatherDetailScreen] 中的 WeatherAlertCard 保持一致：
 * 全部取自 MaterialTheme / ExtendedColors，随七个主题自适应，不硬编码任何色值。
 * - 红色 → error（最高危）
 * - 橙色 → warning
 * - 黄色 → tertiary
 * - 蓝色 / 其他 → primary
 */
@Composable
fun alertLevelColor(level: String): Color {
    return when (level) {
        "红色" -> MaterialTheme.colorScheme.error
        "橙色" -> LocalExtendedColors.current.warning
        "黄色" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

/** 等级严重程度排序权重，数字越大越严重（用于横幅取最紧急的一条）。 */
fun alertSeverity(level: String): Int = when (level) {
    "红色" -> 4
    "橙色" -> 3
    "黄色" -> 2
    "蓝色" -> 1
    else -> 0
}

/**
 * 根据预警类型给出离线防御指南。
 * 关键词匹配，覆盖常见预警类别；未命中时给出通用建议。无 emoji。
 */
fun alertGuidance(type: String): String {
    return when {
        type.contains("暴雨") || type.contains("大雨") ->
            "减少不必要的外出，避开积水与低洼地带；驾车注意路面积水、减速慢行；远离河道、山体等危险区域。"
        type.contains("雷电") ->
            "远离空旷高处、树木与金属物体；室内关好门窗，拔掉不必要的电器电源；避免在户外使用有线电话。"
        type.contains("台风") || type.contains("大风") ->
            "收回或加固阳台、窗台易坠物品；远离临时搭建物、广告牌与大树；减少骑行与高空作业。"
        type.contains("高温") ->
            "避免高温时段长时间户外暴晒；及时补充水分，备好防暑用品；重点关注老人、儿童与体弱者。"
        type.contains("寒潮") || type.contains("低温") || type.contains("霜冻") || type.contains("暴雪") || type.contains("大雪") ->
            "注意添衣保暖，预防呼吸道与心脑血管疾病；水管做好防冻；出行留意积雪结冰路面。"
        type.contains("大雾") || type.contains("霾") ->
            "驾车开启雾灯、降低车速、保持车距；减少户外锻炼；敏感人群佩戴防护口罩。"
        type.contains("冰雹") ->
            "尽快转移到坚固建筑内，远离窗户；户外车辆尽量停入车库或有遮挡处。"
        type.contains("干旱") ->
            "节约用水，注意用火用电安全，防范森林火险。"
        type.contains("沙尘") ->
            "关闭门窗，外出佩戴口罩与护目镜；返回后及时清洁口鼻与裸露皮肤。"
        type.contains("地质灾害") || type.contains("山洪") || type.contains("滑坡") || type.contains("泥石流") ->
            "远离陡坡、河谷与沟谷；听从转移安排，切勿在危险区域停留。"
        else ->
            "关注最新官方预警信息，提前做好防范，减少不必要的外出，注意人身与财产安全。"
    }
}
