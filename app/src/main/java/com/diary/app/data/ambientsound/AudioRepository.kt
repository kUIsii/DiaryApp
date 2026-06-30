package com.diary.app.data.ambientsound

object AudioRepository {
    // CDN base path for Freesound previews (HQ = ~128kbps mp3)
    private const val FS = "https://cdn.freesound.org/previews"

    private fun hq(sid: Int, uid: Int) = "$FS/${sid / 1000}/$sid${"_"}$uid-hq.mp3"

    val categories = listOf(
        AudioCategory("sleep", "助眠",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80"),
        AudioCategory("nature", "自然",
            "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400&q=80"),
        AudioCategory("reading", "伴读",
            "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=400&q=80"),
        AudioCategory("meditation", "冥想",
            "https://images.unsplash.com/photo-1499209974431-9dddcece7f88?w=400&q=80")
    )

    private val allTracks = listOf(
        // Sleep
        AudioTrack("s1", "sleep", "雨打芭蕉", 62, hq(720153, 1504845),
            "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?w=400&q=80"),
        AudioTrack("s2", "sleep", "壁炉篝火", 43, hq(508844, 1934171),
            "https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?w=400&q=80"),
        AudioTrack("s3", "sleep", "海浪白噪音", 180, hq(578524, 5487341),
            "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=400&q=80"),
        AudioTrack("s4", "sleep", "深层宁静", 1860, hq(810260, 5287430),
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400&q=80"),
        AudioTrack("s5", "sleep", "细雨微风", 99, hq(607228, 11069322),
            "https://images.unsplash.com/photo-1438449805896-28a666819a20?w=400&q=80"),
        AudioTrack("s6", "sleep", "炉边夜读", 43, hq(508844, 1934171),
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&q=80"),
        // Nature
        AudioTrack("n1", "nature", "山间溪流", 635, hq(401127, 7724516),
            "https://images.unsplash.com/photo-1433086966358-54859d0ed716?w=400&q=80"),
        AudioTrack("n2", "nature", "森林晨鸟", 367, hq(639985, 5487341),
            "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400&q=80"),
        AudioTrack("n3", "nature", "林风习习", 635, hq(401127, 7724516),
            "https://images.unsplash.com/photo-1440297491022-f9b761d2edbf?w=400&q=80"),
        AudioTrack("n4", "nature", "夏日蝉鸣", 347, hq(573943, 13321649),
            "https://images.unsplash.com/photo-1477414348463-c0eb7f1359b6?w=400&q=80"),
        AudioTrack("n5", "nature", "瀑布水声", 122, hq(489073, 7707368),
            "https://images.unsplash.com/photo-1497294815431-9365093b7331?w=400&q=80"),
        AudioTrack("n6", "nature", "山谷回声", 635, hq(401127, 7724516),
            "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&q=80"),
        // Reading
        AudioTrack("r1", "reading", "月光钢琴", 240, hq(789314, 16936704),
            "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400&q=80"),
        AudioTrack("r2", "reading", "时光吉他", 118, hq(668788, 4717131),
            "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&q=80"),
        AudioTrack("r3", "reading", "低语大提琴", 51, hq(844498, 18268595),
            "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=400&q=80"),
        AudioTrack("r4", "reading", "书页之间", 240, hq(789314, 16936704),
            "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400&q=80"),
        AudioTrack("r5", "reading", "午后阳光", 367, hq(639985, 5487341),
            "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=400&q=80"),
        AudioTrack("r6", "reading", "星空夜曲", 240, hq(789314, 16936704),
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400&q=80"),
        // Meditation
        AudioTrack("m1", "meditation", "颂钵之音", 111, hq(473813, 2979910),
            "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400&q=80"),
        AudioTrack("m2", "meditation", "432Hz谐振", 1860, hq(810260, 5287430),
            "https://images.unsplash.com/photo-1508672019048-805c876b67e2?w=400&q=80"),
        AudioTrack("m3", "meditation", "呼吸引导", 1860, hq(810260, 5287430),
            "https://images.unsplash.com/photo-1545205597-3d9d02c29597?w=400&q=80"),
        AudioTrack("m4", "meditation", "空灵钟声", 111, hq(473813, 2979910),
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&q=80"),
        AudioTrack("m5", "meditation", "竹林风铃", 33, hq(739142, 15326558),
            "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=400&q=80"),
        AudioTrack("m6", "meditation", "晨曦鸟鸣", 367, hq(639985, 5487341),
            "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400&q=80")
    )

    fun getTracks(categoryId: String): List<AudioTrack> = allTracks.filter { it.categoryId == categoryId }
    fun getTrack(id: String): AudioTrack? = allTracks.find { it.id == id }
    fun getAllTracks(): List<AudioTrack> = allTracks
}
