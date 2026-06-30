package com.diary.app.data.ambientsound

object AudioRepository {
    val categories = listOf(
        AudioCategory("sleep", "助眠", "https://images.unsplash.com/photo-1506452305024-9d3f02d1c9b5?w=800"),
        AudioCategory("nature", "自然", "https://images.unsplash.com/photo-1511497584788-876760111969?w=800"),
        AudioCategory("reading", "伴读", "https://images.unsplash.com/photo-1519682577862-e7a8d2b82e3a?w=800"),
        AudioCategory("meditation", "冥想", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800")
    )

    private val allTracks = listOf(
        AudioTrack("s1", "sleep", "雨打芭蕉", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?w=400"),
        AudioTrack("s2", "sleep", "壁炉篝火", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1472712739511-3f2f7fc4f0e1?w=400"),
        AudioTrack("s3", "sleep", "海浪白噪音", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=400"),
        AudioTrack("s4", "sleep", "深层宁静", 5400, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400"),
        AudioTrack("s5", "sleep", "细雨微风", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "https://images.unsplash.com/photo-1493558103817-58b2922d1be6?w=400"),
        AudioTrack("s6", "sleep", "炉边夜读", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "https://images.unsplash.com/photo-1519682577862-e7a8d2b82e3a?w=400"),
        AudioTrack("n1", "nature", "山间溪流", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400"),
        AudioTrack("n2", "nature", "森林晨鸟", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400"),
        AudioTrack("n3", "nature", "林风习习", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400"),
        AudioTrack("n4", "nature", "夏日蝉鸣", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3", "https://images.unsplash.com/photo-1470071459604-7b8ec44ffd4c?w=400"),
        AudioTrack("n5", "nature", "瀑布水声", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3", "https://images.unsplash.com/photo-1504805572947-34fad45aed93?w=400"),
        AudioTrack("n6", "nature", "山谷回声", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3", "https://images.unsplash.com/photo-1585409677983-0f6c41ca9c3b?w=400"),
        AudioTrack("r1", "reading", "月光钢琴", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3", "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400"),
        AudioTrack("r2", "reading", "时光吉他", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3", "https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=400"),
        AudioTrack("r3", "reading", "低语大提琴", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3", "https://images.unsplash.com/photo-1465847899084-d164df4dedc1?w=400"),
        AudioTrack("r4", "reading", "书页之间", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3", "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400"),
        AudioTrack("r5", "reading", "午后阳光", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-17.mp3", "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=400"),
        AudioTrack("r6", "reading", "星空夜曲", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-18.mp3", "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400"),
        AudioTrack("m1", "meditation", "颂钵之音", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-19.mp3", "https://images.unsplash.com/photo-1508672019048-805c876b67e2?w=400"),
        AudioTrack("m2", "meditation", "432Hz谐振", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-20.mp3", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400"),
        AudioTrack("m3", "meditation", "呼吸引导", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1545389336-cf090694435e?w=400"),
        AudioTrack("m4", "meditation", "空灵钟声", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb?w=400"),
        AudioTrack("m5", "meditation", "竹林风铃", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1601370690183-1c7796ecec61?w=400"),
        AudioTrack("m6", "meditation", "晨曦鸟鸣", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400")
    )

    fun getTracks(categoryId: String): List<AudioTrack> = allTracks.filter { it.categoryId == categoryId }
    fun getTrack(id: String): AudioTrack? = allTracks.find { it.id == id }
    fun getAllTracks(): List<AudioTrack> = allTracks
}
