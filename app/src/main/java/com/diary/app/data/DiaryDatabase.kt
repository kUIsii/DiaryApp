package com.diary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DiaryEntry::class, Tag::class, DiaryTag::class, TodoItem::class, TrashEntry::class, 
        DiaryImage::class, CountDownItem::class, HabitRecord::class, TimeCapsule::class, 
        NotificationEntity::class, ChatMessageEntity::class, ChatConversationEntity::class, 
        Achievement::class, TitleDefinition::class, UserTitle::class, TitleProfile::class,
        SmallWin::class, QuickCheckin::class, Goal::class, DiarySummary::class,
        FocusSession::class, CoverTheme::class, DiaryEmbedding::class,
        VoiceMemo::class, EmotionRadar::class, MemoryAnchor::class, AnchorRelation::class,
        WritingFingerprint::class, TrackedPerson::class, PersonMention::class, Decision::class,
        ExtractedValue::class, WritingExperiment::class, ExperimentParticipation::class,
        MonthlyChallenge::class, ChallengeDailyLog::class, StreakShield::class
    ],
    version = 36,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun titleDao(): TitleDao

    companion object {
        class DiaryDatabaseOpenException(
            message: String,
            cause: Throwable
        ) : IllegalStateException(message, cause)

        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1→2: no schema changes, placeholder to complete migration chain
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        priority INTEGER NOT NULL DEFAULT 0,
                        dueDate INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_createdAt ON diary_entries (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_isFavorite ON diary_entries (isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_moodLevel ON diary_entries (moodLevel)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tag_cross_ref_tagId ON diary_tag_cross_ref (tagId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tag_cross_ref_diaryId ON diary_tag_cross_ref (diaryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_dueDate ON todo_items (dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_isCompleted ON todo_items (isCompleted)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN category TEXT NOT NULL DEFAULT 'task'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_category ON todo_items (category)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN reminderTime INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_reminderTime ON todo_items (reminderTime)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS trash_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        originalId INTEGER NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL DEFAULT '',
                        plainText TEXT NOT NULL DEFAULT '',
                        moodLevel INTEGER,
                        weather TEXT,
                        location TEXT,
                        latitude REAL,
                        longitude REAL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trash_entries_deletedAt ON trash_entries (deletedAt)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN parentId INTEGER")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN recurringType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN progress INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_parentId ON todo_items (parentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_tags ON todo_items (tags)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diary_images (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryId INTEGER NOT NULL,
                        localPath TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (entryId) REFERENCES diary_entries(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_images_entryId ON diary_images (entryId)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS countdown_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        targetDate INTEGER NOT NULL,
                        isCountUp INTEGER NOT NULL DEFAULT 0,
                        color INTEGER NOT NULL DEFAULT 1234006169,
                        isRepeatYearly INTEGER NOT NULL DEFAULT 0,
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN linkedTagIds TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        todoId INTEGER NOT NULL,
                        recordDate INTEGER NOT NULL,
                        source TEXT NOT NULL DEFAULT 'manual',
                        summary TEXT NOT NULL DEFAULT '',
                        diaryEntryId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_records_todoId_recordDate ON habit_records (todoId, recordDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_records_recordDate ON habit_records (recordDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_records_diaryEntryId ON habit_records (diaryEntryId)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_images ADD COLUMN thumbPath TEXT")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN mediaName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN mediaRef TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN mimeType TEXT NOT NULL DEFAULT 'image/jpeg'")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_capsules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        unlockDate INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL,
                        iconType TEXT NOT NULL,
                        colorHex INTEGER NOT NULL,
                        relatedId INTEGER,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        isTrashed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        trashedAt INTEGER
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_type ON notifications (type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_isTrashed ON notifications (isTrashed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_createdAt ON notifications (createdAt)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_capsules ADD COLUMN isOpened INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE time_capsules ADD COLUMN theme TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE time_capsules ADD COLUMN imageUri TEXT")
                db.execSQL("ALTER TABLE time_capsules ADD COLUMN unlockHour INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE time_capsules ADD COLUMN unlockMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL DEFAULT '新对话',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN conversationId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversationId ON chat_messages (conversationId)")
                db.execSQL("INSERT INTO chat_conversations (title, createdAt, updatedAt) VALUES ('默认对话', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                db.execSQL("UPDATE chat_messages SET conversationId = (SELECT id FROM chat_conversations LIMIT 1)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        key TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        iconEmoji TEXT NOT NULL,
                        unlockedAt INTEGER,
                        progress INTEGER NOT NULL DEFAULT 0,
                        target INTEGER NOT NULL DEFAULT 1
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_achievements_key ON achievements (key)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE achievements RENAME COLUMN iconEmoji TO iconName")
                val iconMap = mapOf(
                    "first_entry" to "AutoFixHigh",
                    "entries_10" to "HistoryEdu",
                    "entries_50" to "MilitaryTech",
                    "entries_100" to "Whatshot",
                    "streak_7" to "TrendingUp",
                    "streak_30" to "DateRange",
                    "words_10000" to "TextSnippet",
                    "words_100000" to "AutoStories",
                    "moods_5" to "SentimentSatisfied",
                    "all_weather" to "Thunderstorm",
                    "night_writer" to "NightsStay",
                    "early_bird" to "LightMode",
                    "favorite_1" to "BookmarkAdded",
                    "favorites_10" to "CollectionsBookmark",
                    "tags_5" to "NewLabel",
                    "images_10" to "PhotoLibrary"
                )
                iconMap.forEach { (key, iconName) ->
                    db.execSQL("UPDATE achievements SET iconName = '$iconName' WHERE key = '$key'")
                }
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) { }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN writing_duration_seconds INTEGER")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS title_definitions (
                        key TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        iconName TEXT NOT NULL,
                        tier INTEGER NOT NULL DEFAULT 1,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        flavorText TEXT NOT NULL DEFAULT ''
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_title_definitions_category ON title_definitions (category)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_titles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titleKey TEXT NOT NULL,
                        unlockedAt INTEGER NOT NULL,
                        relatedEntryId INTEGER
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_titles_titleKey ON user_titles (titleKey)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS title_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        activeTitleKey TEXT,
                        showTitleOnHome INTEGER NOT NULL DEFAULT 0,
                        showTitleOnEntry INTEGER NOT NULL DEFAULT 0
                    )
                """)

                val titles = com.diary.app.data.TitleSeedData.allTitles
                for (title in titles) {
                    db.execSQL(
                        """INSERT OR IGNORE INTO title_definitions (key, name, description, category, iconName, tier, isHidden, flavorText)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                        arrayOf(title.key, title.name, title.description, title.category, title.iconName, title.tier, if (title.isHidden) 1 else 0, title.flavorText)
                    )
                }
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pet_personality (
                        id INTEGER PRIMARY KEY NOT NULL,
                        extraversion REAL NOT NULL DEFAULT 0.5,
                        openness REAL NOT NULL DEFAULT 0.5,
                        conscientiousness REAL NOT NULL DEFAULT 0.5,
                        agreeableness REAL NOT NULL DEFAULT 0.5,
                        emotional_stability REAL NOT NULL DEFAULT 0.5,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pet_states (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entry_id INTEGER NOT NULL,
                        state TEXT NOT NULL,
                        trigger TEXT NOT NULL,
                        feedback_text TEXT NOT NULL,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pet_states_entry_id ON pet_states (entry_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pet_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL DEFAULT '小记',
                        current_state TEXT NOT NULL DEFAULT 'CALM',
                        affection INTEGER NOT NULL DEFAULT 0,
                        last_interaction INTEGER NOT NULL DEFAULT 0,
                        last_entry_time INTEGER NOT NULL DEFAULT 0,
                        streak_days INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("INSERT OR IGNORE INTO pet_profile (id, name, current_state, affection, last_interaction, last_entry_time, streak_days) VALUES (1, '小记', 'CALM', 0, 0, 0, 0)")
                db.execSQL("INSERT OR IGNORE INTO pet_personality (id, extraversion, openness, conscientiousness, agreeableness, emotional_stability, updated_at) VALUES (1, 0.5, 0.5, 0.5, 0.5, 0.5, 0)")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_environment (
                        id INTEGER PRIMARY KEY NOT NULL,
                        lushness REAL NOT NULL DEFAULT 0.3,
                        brightness REAL NOT NULL DEFAULT 0.5,
                        tranquility REAL NOT NULL DEFAULT 0.5,
                        warmth REAL NOT NULL DEFAULT 0.5,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        experience INTEGER NOT NULL DEFAULT 0,
                        total_entries INTEGER NOT NULL DEFAULT 0,
                        streak_days INTEGER NOT NULL DEFAULT 0,
                        last_entry_time INTEGER NOT NULL DEFAULT 0,
                        unlocked_decorations TEXT NOT NULL DEFAULT '[]',
                        active_decorations TEXT NOT NULL DEFAULT '[]'
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_decorations (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        unlock_level INTEGER NOT NULL,
                        unlock_condition TEXT NOT NULL,
                        type TEXT NOT NULL,
                        layer INTEGER NOT NULL,
                        pos_x REAL NOT NULL DEFAULT 0.5,
                        pos_y REAL NOT NULL DEFAULT 0.5,
                        is_unlocked INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_updates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entry_id INTEGER NOT NULL,
                        lushness_delta REAL NOT NULL DEFAULT 0,
                        brightness_delta REAL NOT NULL DEFAULT 0,
                        tranquility_delta REAL NOT NULL DEFAULT 0,
                        warmth_delta REAL NOT NULL DEFAULT 0,
                        experience_gained INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_island_updates_entry_id ON island_updates (entry_id)")

                db.execSQL("INSERT OR IGNORE INTO island_profile (id, level, experience, total_entries, streak_days, last_entry_time, unlocked_decorations, active_decorations) VALUES (1, 1, 0, 0, 0, 0, '[]', '[]')")
                db.execSQL("INSERT OR IGNORE INTO island_environment (id, lushness, brightness, tranquility, warmth, updated_at) VALUES (1, 0.3, 0.5, 0.5, 0.5, 0)")

                val decorations = listOf(
                    arrayOf("cabin", "小木屋", 5, "达到5级解锁", "building", 4, 0.5, 0.6, 0),
                    arrayOf("lighthouse", "灯塔", 15, "连续7天记录", "building", 4, 0.8, 0.4, 0),
                    arrayOf("bridge", "桥梁", 20, "达到20级解锁", "building", 4, 0.3, 0.7, 0),
                    arrayOf("fountain", "喷泉", 25, "使用5种心情", "building", 4, 0.6, 0.5, 0),
                    arrayOf("statue", "守护者雕像", 40, "连续30天记录", "building", 4, 0.5, 0.3, 0),
                    arrayOf("bird", "小鸟", 3, "达到3级解锁", "animal", 5, 0.2, 0.2, 0),
                    arrayOf("butterfly", "蝴蝶", 8, "积极心情解锁", "animal", 5, 0.7, 0.3, 0),
                    arrayOf("squirrel", "松鼠", 12, "达到12级解锁", "animal", 5, 0.4, 0.5, 0),
                    arrayOf("owl", "猫头鹰", 18, "夜间写作解锁", "animal", 5, 0.8, 0.2, 0),
                    arrayOf("dragon", "巨龙", 50, "连续100天记录", "animal", 5, 0.5, 0.1, 0),
                    arrayOf("flowers", "花海", 6, "达到6级解锁", "vegetation", 3, 0.3, 0.6, 0),
                    arrayOf("tree", "大树", 10, "达到10级解锁", "vegetation", 3, 0.7, 0.5, 0),
                    arrayOf("grass", "草地", 2, "达到2级解锁", "vegetation", 3, 0.5, 0.8, 0),
                    arrayOf("rainbow", "彩虹", 30, "达到30级解锁", "effect", 6, 0.5, 0.1, 0),
                    arrayOf("fireflies", "萤火虫", 22, "达到22级解锁", "effect", 6, 0.3, 0.4, 0),
                    arrayOf("aurora", "极光", 35, "达到35级解锁", "effect", 6, 0.5, 0.05, 0)
                )

                for (d in decorations) {
                    db.execSQL(
                        """INSERT OR IGNORE INTO island_decorations (id, name, unlock_level, unlock_condition, type, layer, pos_x, pos_y, is_unlocked)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                        d
                    )
                }
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pet_profile ADD COLUMN growth_stage TEXT NOT NULL DEFAULT 'JUVENILE'")
                db.execSQL("ALTER TABLE pet_profile ADD COLUMN evolved_at INTEGER")
                db.execSQL("ALTER TABLE pet_profile ADD COLUMN discovered_hidden_states TEXT NOT NULL DEFAULT '[]'")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pet_memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        content TEXT NOT NULL,
                        strength REAL NOT NULL DEFAULT 0.5,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        last_activated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pet_memory_type ON pet_memory (type)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pet_hidden_states (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        state_type TEXT NOT NULL,
                        first_discovered_at INTEGER NOT NULL DEFAULT 0,
                        is_active INTEGER NOT NULL DEFAULT 0,
                        activation_count INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pet_hidden_states_state_type ON pet_hidden_states (state_type)")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pet_memory ADD COLUMN related_entry_id INTEGER")
                db.execSQL("ALTER TABLE pet_memory ADD COLUMN trigger_text TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_discoveries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        discovery_type TEXT NOT NULL,
                        discovery_key TEXT NOT NULL,
                        discovered_at INTEGER NOT NULL DEFAULT 0,
                        message TEXT NOT NULL,
                        expires_at INTEGER NOT NULL DEFAULT -1,
                        metadata TEXT
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_combos (
                        id TEXT NOT NULL PRIMARY KEY,
                        comboId TEXT NOT NULL,
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        unlockedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS island_combos")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_combos (
                        id TEXT NOT NULL PRIMARY KEY,
                        comboId TEXT NOT NULL,
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        unlockedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建小岛历史时间线事件表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS island_timeline_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        event_type TEXT NOT NULL,
                        message TEXT NOT NULL,
                        event_time INTEGER NOT NULL DEFAULT 0,
                        related_entry_id INTEGER,
                        metadata TEXT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_island_timeline_events_event_time ON island_timeline_events (event_time)")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add unified achievement system columns
                db.execSQL("ALTER TABLE achievements ADD COLUMN category TEXT NOT NULL DEFAULT 'writing'")
                db.execSQL("ALTER TABLE achievements ADD COLUMN tier INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE achievements ADD COLUMN iconEmoji TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE achievements ADD COLUMN flavorText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE achievements ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")

                // Seed all unified achievement definitions
                val seeds = listOf(
                    // WRITING
                    arrayOf("first_entry", "初出茅庐", "写下第一篇日记", "Star", "WRITING", 1, "\uD83C\uDF1F", "每一段旅程都有第一步，你的第一步从这里开始。", 1, 0),
                    arrayOf("entries_10", "笔耕不辍", "累计写下 10 篇日记", "Star", "WRITING", 1, "\uD83D\uDCDD", "10篇日记，你已经养成了记录的习惯。文字是你最好的朋友。", 10, 0),
                    arrayOf("entries_50", "日记达人", "累计写下 50 篇日记", "Star", "WRITING", 2, "\uD83D\uDCDA", "50篇日记，你的文字已经可以编成一本小书了。坚持就是力量。", 50, 0),
                    arrayOf("entries_100", "百篇里程碑", "累计写下 100 篇日记", "Star", "WRITING", 3, "\uD83C\uDF1F", "100篇日记，你已经是一位真正的记录者。", 100, 0),
                    arrayOf("thousand_words", "千字长文", "单篇日记超过 1000 字", "Star", "WRITING", 2, "\uD83D\uDCDD", "洋洋洒洒千字文，你的思考深度令人敬佩。", 1000, 0),
                    arrayOf("fifty_thousand_words", "五万字著者", "累计写作超过 50000 字", "Star", "WRITING", 3, "\uD83D\uDCD6", "5万字，你已经可以出一本小书了。", 50000, 0),
                    arrayOf("brief_master", "短句大师", "收藏一篇不超过 50 字的日记", "Star", "WRITING", 1, "\u2728", "言简意赅，字字珠玑。", 1, 0),
                    arrayOf("flash_writer", "闪念速记", "在极短时间内完成一篇日记", "Star", "WRITING", 2, "\u26A1", "灵感转瞬即逝，你抓住了它。", 1, 1),
                    arrayOf("deep_writer", "深度思考者", "单篇日记超过 3000 字", "Star", "WRITING", 3, "\uD83E\uDDE0", "3000字的深度思考，你是一位真正的思想者。", 3000, 1),
                    // HABIT
                    arrayOf("streak_7", "七日连续", "连续 7 天写日记", "Star", "HABIT", 1, "\uD83D\uDD25", "7天不间断，习惯已经养成。", 7, 0),
                    arrayOf("streak_30", "月度坚持", "连续 30 天写日记", "Star", "HABIT", 2, "\uD83D\uDD25", "30天的坚持，你已经超越了大多数人。", 30, 0),
                    arrayOf("daily_writer", "日常记录者", "连续 14 天写日记", "Star", "HABIT", 2, "\uD83D\uDCD6", "14天的坚持，写作已经成为你生活的一部分。", 14, 0),
                    arrayOf("hundred_days", "百日征程", "连续 100 天写日记", "Star", "HABIT", 4, "\uD83C\uDFC6", "100天不间断，你已经创造了属于自己的传奇。", 100, 0),
                    arrayOf("returnee", "回归者", "中断后重新开始写日记", "Star", "HABIT", 1, "\uD83C\uDF31", "重新开始比坚持更难，你做到了。", 1, 0),
                    // TIME
                    arrayOf("night_writer", "深夜笔者", "在凌晨 0-4 点写日记", "Star", "TIME", 1, "\uD83C\uDF19", "夜深人静，思绪最清晰。", 1, 0),
                    arrayOf("early_bird", "晨曦记录者", "在清晨 5-6 点写日记", "Star", "TIME", 1, "\uD83C\uDF05", "早起的鸟儿有虫吃，早起的你有故事写。", 1, 0),
                    arrayOf("night_poet", "午夜诗人", "在凌晨 0-2 点写 5 篇日记", "Star", "TIME", 2, "\uD83C\uDF19", "午夜的诗人，用文字编织星光。", 5, 0),
                    arrayOf("dawn_recorder", "黎明记录者", "在凌晨 3-4 点写 3 篇日记", "Star", "TIME", 2, "\uD83C\uDF05", "黎明前的记录者，捕捉夜与昼的交界。", 3, 0),
                    arrayOf("morning_writer", "清晨写手", "在早晨 5-7 点写 10 篇日记", "Star", "TIME", 3, "\u2600\uFE0F", "每一个清晨都是新的开始，你用文字迎接它。", 10, 0),
                    arrayOf("weekday_killer", "全周覆盖", "在一周的每一天都写过日记", "Star", "TIME", 2, "\uD83D\uDCC5", "周一到周日，每一天都有你的记录。", 7, 0),
                    // MOOD
                    arrayOf("moods_5", "五彩心情", "使用 5 种不同心情", "Star", "MOOD", 1, "\uD83C\uDFA8", "你的情感世界丰富多彩。", 5, 0),
                    arrayOf("mood_palette", "心情调色盘", "使用 8 种不同心情", "Star", "MOOD", 2, "\uD83C\uDFA8", "8种心情，你的情感光谱完整而美丽。", 8, 0),
                    arrayOf("optimist", "乐观达人", "连续 5 篇高心情日记", "Star", "MOOD", 2, "\uD83D\uDE04", "连续的快乐，你是生活的乐观主义者。", 5, 0),
                    arrayOf("deep_thinker", "深沉思考者", "连续 5 篇低心情日记", "Star", "MOOD", 2, "\uD83E\uDD14", "深度的思考往往伴随着沉静的心情。", 5, 1),
                    arrayOf("calm_sea", "平静之海", "连续 5 篇平静心情日记", "Star", "MOOD", 1, "\uD83C\uDF0A", "平静如海，内心安宁。", 5, 0),
                    // WEATHER
                    arrayOf("all_weather", "风雨无阻", "在所有天气类型下都写过日记", "Star", "WEATHER", 2, "\u26C5", "无论风雨晴雪，你都在记录。", 5, 0),
                    arrayOf("rain_collector", "雨天收集者", "在雨天写 5 篇日记", "Star", "WEATHER", 1, "\uD83C\uDF27\uFE0F", "雨天的思绪格外绵长。", 5, 0),
                    arrayOf("snow_writer", "雪夜笔者", "在雪天写 3 篇日记", "Star", "WEATHER", 2, "\u2744\uFE0F", "雪夜的安静适合写作。", 3, 0),
                    arrayOf("storm_writer", "风暴记录者", "在大风天气写 3 篇日记", "Star", "WEATHER", 2, "\uD83C\uDF29\uFE0F", "风暴中的记录者，勇敢而坚定。", 3, 0),
                    arrayOf("sunny_recorder", "晴天记录者", "在晴天写 10 篇日记", "Star", "WEATHER", 1, "\u2600\uFE0F", "阳光下的文字总是温暖的。", 10, 0),
                    // EXPLORER
                    arrayOf("photo_diary", "图文并茂", "在 3 篇日记中添加图片", "Star", "EXPLORER", 1, "\uD83D\uDCF7", "图片让记忆更加生动。", 3, 0),
                    arrayOf("twin_stars", "双子星", "同一天写 2 篇日记", "Star", "EXPLORER", 2, "\u2B50", "一天两篇，你的表达欲旺盛。", 2, 1),
                    // COLLECTOR
                    arrayOf("favorite_1", "初次收藏", "收藏第一篇日记", "Star", "COLLECTOR", 1, "\u2764\uFE0F", "每一篇被收藏的日记都有特别的意义。", 1, 0),
                    arrayOf("favorites_10", "收藏家", "收藏 10 篇日记", "Star", "COLLECTOR", 2, "\uD83D\uDCDC", "10篇收藏，你珍藏了许多美好时刻。", 10, 0),
                    arrayOf("collector", "记忆守护者", "收藏 20 篇日记", "Star", "COLLECTOR", 3, "\uD83D\uDCDC", "20篇收藏，你是记忆的守护者。", 20, 0),
                    arrayOf("tags_5", "标签达人", "使用 5 个不同标签", "Star", "COLLECTOR", 1, "\uD83C\uDFF7\uFE0F", "标签让日记更有条理。", 5, 0),
                    arrayOf("images_10", "影像记忆", "添加 10 张图片", "Star", "COLLECTOR", 2, "\uD83D\uDDBC\uFE0F", "10张图片，视觉记忆永不褪色。", 10, 0),
                    // LEGENDARY
                    arrayOf("legendary_entries_500", "五百篇传说", "累计写下 500 篇日记", "Star", "LEGENDARY", 4, "\uD83C\uDFC6", "500篇日记，你已经创造了属于自己的文字传奇。", 500, 0),
                    arrayOf("legendary_streak_365", "年度不间断", "连续 365 天写日记", "Star", "LEGENDARY", 4, "\uD83C\uDFC5", "365天不间断，你是时间的征服者。", 365, 0),
                    arrayOf("legendary_words_million", "百万字著", "累计写作超过 1000000 字", "Star", "LEGENDARY", 4, "\uD83D\uDCDA", "百万字，你已经是一位真正的作家。", 1000000, 0),
                    arrayOf("legendary_all_categories", "全能记录者", "解锁所有类别的成就", "Star", "LEGENDARY", 4, "\uD83C\uDFC6", "全部类别解锁，你是真正的全能记录者。", 8, 1)
                )

                for (s in seeds) {
                    db.execSQL(
                        """INSERT OR IGNORE INTO achievements (key, name, description, iconName, category, tier, iconEmoji, flavorText, target, isHidden, progress)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)""",
                        s
                    )
                }

                // Update existing old-system achievements with category/tier metadata
                val oldUpdates = mapOf(
                    "first_entry" to arrayOf("WRITING", 1, "\uD83C\uDF1F", "每一段旅程都有第一步，你的第一步从这里开始。", 1, 0),
                    "entries_10" to arrayOf("WRITING", 1, "\uD83D\uDCDD", "10篇日记，你已经养成了记录的习惯。文字是你最好的朋友。", 10, 0),
                    "entries_50" to arrayOf("WRITING", 2, "\uD83D\uDCDA", "50篇日记，你的文字已经可以编成一本小书了。坚持就是力量。", 50, 0),
                    "entries_100" to arrayOf("WRITING", 3, "\uD83C\uDF1F", "100篇日记，你已经是一位真正的记录者。", 100, 0),
                    "streak_7" to arrayOf("HABIT", 1, "\uD83D\uDD25", "7天不间断，习惯已经养成。", 7, 0),
                    "streak_30" to arrayOf("HABIT", 2, "\uD83D\uDD25", "30天的坚持，你已经超越了大多数人。", 30, 0),
                    "words_10000" to arrayOf("WRITING", 2, "\uD83D\uDCD6", "一万字的积累，你的文字功底日渐深厚。", 10000, 0),
                    "words_100000" to arrayOf("WRITING", 3, "\uD83D\uDCDA", "十万字，你已经可以出一本厚厚的日记集了。", 100000, 0),
                    "moods_5" to arrayOf("MOOD", 1, "\uD83C\uDFA8", "你的情感世界丰富多彩。", 5, 0),
                    "all_weather" to arrayOf("WEATHER", 2, "\u26C5", "无论风雨晴雪，你都在记录。", 5, 0),
                    "night_writer" to arrayOf("TIME", 1, "\uD83C\uDF19", "夜深人静，思绪最清晰。", 1, 0),
                    "early_bird" to arrayOf("TIME", 1, "\uD83C\uDF05", "早起的鸟儿有虫吃，早起的你有故事写。", 1, 0),
                    "favorite_1" to arrayOf("COLLECTOR", 1, "\u2764\uFE0F", "每一篇被收藏的日记都有特别的意义。", 1, 0),
                    "favorites_10" to arrayOf("COLLECTOR", 2, "\uD83D\uDCDC", "10篇收藏，你珍藏了许多美好时刻。", 10, 0),
                    "tags_5" to arrayOf("COLLECTOR", 1, "\uD83C\uDFF7\uFE0F", "标签让日记更有条理。", 5, 0),
                    "images_10" to arrayOf("COLLECTOR", 2, "\uD83D\uDDBC\uFE0F", "10张图片，视觉记忆永不褪色。", 10, 0)
                )
                for ((key, values) in oldUpdates) {
                    db.execSQL(
                        """UPDATE achievements SET category = ?, tier = ?, iconEmoji = ?, flavorText = ?, target = ?, isHidden = ? WHERE key = ?""",
                        arrayOf(values[0], values[1], values[2], values[3], values[4], values[5], key)
                    )
                }
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create small_wins table for daily small victories tracking
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS small_wins (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        recordDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_small_wins_recordDate ON small_wins (recordDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_small_wins_createdAt ON small_wins (createdAt)")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Quick checkins
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS quick_checkins (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        moodLevel INTEGER,
                        photoUri TEXT,
                        text TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_quick_checkins_createdAt ON quick_checkins (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_quick_checkins_moodLevel ON quick_checkins (moodLevel)")

                // Goals
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        parentId INTEGER,
                        progress INTEGER NOT NULL DEFAULT 0,
                        targetValue INTEGER NOT NULL DEFAULT 100,
                        unit TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_parentId ON goals (parentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_createdAt ON goals (createdAt)")

                // Diary summaries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diary_summaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        summary TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_diary_summaries_diaryId ON diary_summaries (diaryId)")

                // Focus sessions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS focus_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        durationMinutes INTEGER NOT NULL DEFAULT 25,
                        wordCountGoal INTEGER,
                        ambientSound TEXT,
                        completedAt INTEGER
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_startTime ON focus_sessions (startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_endTime ON focus_sessions (endTime)")

                // Cover themes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cover_themes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        texturePath TEXT,
                        fontFamily TEXT,
                        accentColor INTEGER,
                        isActive INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cover_themes_isActive ON cover_themes (isActive)")

                // Diary embeddings
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diary_embeddings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        embeddingJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_diary_embeddings_diaryId ON diary_embeddings (diaryId)")

                // Location memories
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS location_memories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        radiusMeters REAL NOT NULL DEFAULT 100.0,
                        locationName TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_memories_latitude_longitude ON location_memories (latitude, longitude)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_memories_diaryId ON location_memories (diaryId)")

                // Voice memos
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS voice_memos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER,
                        audioPath TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        transcript TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_voice_memos_diaryId ON voice_memos (diaryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_voice_memos_createdAt ON voice_memos (createdAt)")
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Emotion radar
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS emotion_radar (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        vitality REAL NOT NULL DEFAULT 0.5,
                        calmness REAL NOT NULL DEFAULT 0.5,
                        happiness REAL NOT NULL DEFAULT 0.5,
                        gratitude REAL NOT NULL DEFAULT 0.5,
                        socialConnection REAL NOT NULL DEFAULT 0.5,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_emotion_radar_diaryId ON emotion_radar (diaryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_emotion_radar_createdAt ON emotion_radar (createdAt)")

                // Memory anchors
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS memory_anchors (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        topic TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_memory_anchors_diaryId ON memory_anchors (diaryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_anchors_topic ON memory_anchors (topic)")

                // Anchor relations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS anchor_relations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        anchorId INTEGER NOT NULL,
                        diaryId INTEGER NOT NULL,
                        relevanceScore REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_anchor_relations_anchorId ON anchor_relations (anchorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_anchor_relations_diaryId ON anchor_relations (diaryId)")

                // Writing fingerprints
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS writing_fingerprints (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        periodStart INTEGER NOT NULL,
                        avgSentenceLength REAL NOT NULL DEFAULT 0.0,
                        vocabularyRichness REAL NOT NULL DEFAULT 0.0,
                        avgWordLength REAL NOT NULL DEFAULT 0.0,
                        punctuationRatio REAL NOT NULL DEFAULT 0.0,
                        paragraphCount INTEGER NOT NULL DEFAULT 0,
                        avgParagraphLength REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_writing_fingerprints_periodStart ON writing_fingerprints (periodStart)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_writing_fingerprints_diaryId ON writing_fingerprints (diaryId)")

                // Tracked persons
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tracked_persons (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        mentionCount INTEGER NOT NULL DEFAULT 0,
                        lastMentionedAt INTEGER,
                        avgSentiment REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracked_persons_name ON tracked_persons (name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracked_persons_createdAt ON tracked_persons (createdAt)")

                // Person mentions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS person_mentions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personId INTEGER NOT NULL,
                        diaryId INTEGER NOT NULL,
                        context TEXT NOT NULL DEFAULT '',
                        sentiment REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_person_mentions_personId ON person_mentions (personId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_person_mentions_diaryId ON person_mentions (diaryId)")

                // Decisions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS decisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaryId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        context TEXT NOT NULL DEFAULT '',
                        options TEXT NOT NULL DEFAULT '',
                        chosenOption TEXT NOT NULL DEFAULT '',
                        concerns TEXT NOT NULL DEFAULT '',
                        madeAt INTEGER NOT NULL DEFAULT 0,
                        followUpAt INTEGER,
                        outcome TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_decisions_diaryId ON decisions (diaryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_decisions_madeAt ON decisions (madeAt)")

                // Extracted values
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS extracted_values (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        value TEXT NOT NULL,
                        evidence TEXT NOT NULL DEFAULT '',
                        confidence REAL NOT NULL DEFAULT 0.0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_extracted_values_category ON extracted_values (category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_extracted_values_updatedAt ON extracted_values (updatedAt)")

                // Writing experiments
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS writing_experiments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        rules TEXT NOT NULL,
                        badgeName TEXT NOT NULL DEFAULT '',
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'upcoming',
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_writing_experiments_startDate ON writing_experiments (startDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_writing_experiments_status ON writing_experiments (status)")

                // Experiment participations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS experiment_participations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        experimentId INTEGER NOT NULL,
                        diaryId INTEGER,
                        dayNumber INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        completedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_experiment_participations_experimentId ON experiment_participations (experimentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_experiment_participations_diaryId ON experiment_participations (diaryId)")

                // Monthly challenges
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_challenges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        targetDays INTEGER NOT NULL DEFAULT 20,
                        completedDays INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'upcoming',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_monthly_challenges_year_month ON monthly_challenges (year, month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_monthly_challenges_status ON monthly_challenges (status)")

                // Challenge daily logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS challenge_daily_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        challengeId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL DEFAULT '',
                        diaryId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_challenge_daily_logs_challengeId_date ON challenge_daily_logs (challengeId, date)")

                // Streak shields
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS streak_shields (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        month TEXT NOT NULL,
                        usedAt INTEGER,
                        savedDate INTEGER,
                        isUsed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_shields_month ON streak_shields (month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_shields_usedAt ON streak_shields (usedAt)")

                // Easter eggs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS easter_eggs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eggId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        triggeredAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_easter_eggs_eggId ON easter_eggs (eggId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_easter_eggs_triggeredAt ON easter_eggs (triggeredAt)")
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS location_memories")
                db.execSQL("DROP TABLE IF EXISTS easter_eggs")
            }
        }

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val allMigrations = arrayOf(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                    MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
                    MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26,
                    MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30,
                    MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35,
                    MIGRATION_35_36
                )
                val callback = object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try { db.execSQL("DROP INDEX IF EXISTS index_countdown_items_targetDate") } catch (e: Exception) {
                            android.util.Log.w("DiaryDatabase", "Failed to drop index", e)
                        }
                        try { backfillDiaryImages(db, context) } catch (e: Exception) {
                            android.util.Log.w("DiaryDatabase", "Failed to backfill diary images", e)
                        }
                    }
                }

                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        DiaryDatabase::class.java,
                        "diary_database"
                    ).addMigrations(*allMigrations)
                    .addCallback(callback)
                    .build()
                } catch (e: Exception) {
                    android.util.Log.e("DiaryDatabase", "Migration failed, backing up database before surfacing open error", e)
                    try {
                        val dbFile = context.getDatabasePath("diary_database")
                        val backupDir = java.io.File(context.filesDir, "db_backup")
                        if (!backupDir.exists()) backupDir.mkdirs()
                        val timestamp = System.currentTimeMillis()
                        val suffixes = listOf("", "-wal", "-shm", "-journal")
                        for (suffix in suffixes) {
                            val src = java.io.File(dbFile.parentFile, "diary_database$suffix")
                            if (src.exists()) {
                                val dst = java.io.File(backupDir, "diary_database${suffix}_$timestamp")
                                src.copyTo(dst, overwrite = true)
                            }
                        }
                        android.util.Log.e("DiaryDatabase", "Database backed up to: ${backupDir.absolutePath}/diary_database*_$timestamp")
                    } catch (backupError: Exception) {
                        android.util.Log.e("DiaryDatabase", "Failed to backup database files", backupError)
                    }
                    throw DiaryDatabaseOpenException(
                        message = "Unable to open diary database safely. A backup was created before aborting startup.",
                        cause = e
                    )
                }
                INSTANCE = instance
                instance
            }
        }

        private fun backfillDiaryImages(db: SupportSQLiteDatabase, context: Context) {
            db.query("SELECT COUNT(*) FROM diary_images").use { countCursor ->
                val imageCount = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
                if (imageCount > 0) return
            }

            val mediaDir = java.io.File(context.filesDir, "diary_media")
            val thumbDir = java.io.File(mediaDir, "thumbs")

            db.query("SELECT id, content FROM diary_entries").use { cursor ->
                val idIdx = cursor.getColumnIndex("id")
                val contentIdx = cursor.getColumnIndex("content")
                val now = System.currentTimeMillis()
                var backfillCount = 0

                while (cursor.moveToNext()) {
                    val entryId = cursor.getLong(idIdx)
                    val content = cursor.getString(contentIdx) ?: continue
                    if (content.isBlank()) continue

                    val names = linkedSetOf<String>()
                    Regex("diary-media://([^\"'\\s}]+)").findAll(content).forEach {
                        names.add(it.groupValues[1])
                    }
                    Regex("https://appassets/diary_media/((?!thumbs/)[^\"'\\s}]+)").findAll(content).forEach {
                        names.add(it.groupValues[1])
                    }
                    Regex("file://([^\"']*diary_media[\\\\/]((?!thumbs[\\\\/])[^\"'\\\\/]+))").findAll(content).forEach {
                        names.add(it.groupValues[2])
                    }

                    names.forEachIndexed { index, mediaName ->
                        val displayFile = java.io.File(mediaDir, mediaName)
                        val thumbFile = java.io.File(thumbDir, mediaName)
                        val localPath = displayFile.absolutePath
                        val thumbPath = if (thumbFile.exists()) thumbFile.absolutePath else null

                        db.execSQL(
                            """INSERT OR IGNORE INTO diary_images
                               (entryId, localPath, thumbPath, mediaName, mediaRef, mimeType, fileSize, sortOrder, createdAt)
                               VALUES (?, ?, ?, ?, ?, 'image/jpeg', ?, ?, ?)""",
                            arrayOf(entryId, localPath, thumbPath, mediaName, "diary-media://$mediaName",
                                if (displayFile.exists()) displayFile.length() else 0L, index, now)
                        )
                        backfillCount++
                    }
                }

                if (backfillCount > 0) {
                    android.util.Log.d("DiaryDatabase", "Backfilled $backfillCount diary_images for existing entries")
                }
            }
        }
    }
}
