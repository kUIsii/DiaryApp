package com.diary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntry::class, Tag::class, DiaryTag::class, TodoItem::class, TrashEntry::class, DiaryImage::class, CountDownItem::class, HabitRecord::class, TimeCapsule::class, NotificationEntity::class, ChatMessageEntity::class, ChatConversationEntity::class, Achievement::class],
    version = 20,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun achievementDao(): AchievementDao

    companion object {
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
                // Indices for diary_entries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_createdAt ON diary_entries (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_isFavorite ON diary_entries (isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_moodLevel ON diary_entries (moodLevel)")
                // Indices for diary_tag_cross_ref
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tag_cross_ref_tagId ON diary_tag_cross_ref (tagId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tag_cross_ref_diaryId ON diary_tag_cross_ref (diaryId)")
                // Indices for todo_items
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
                // Add new columns to todo_items
                db.execSQL("ALTER TABLE todo_items ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN parentId INTEGER")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN recurringType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN progress INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todo_items ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                // Create indices for new columns
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
                // Create conversations table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL DEFAULT '新对话',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                // Add conversationId to chat_messages
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN conversationId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversationId ON chat_messages (conversationId)")
                // Create a default conversation for existing messages
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

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val allMigrations = arrayOf(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                    MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20
                )
                val callback = object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try { db.execSQL("DROP INDEX IF EXISTS index_countdown_items_targetDate") } catch (e: Exception) {
                            android.util.Log.w("DiaryDatabase", "Failed to drop index", e)
                        }
                        // Backfill diary_images for entries that were saved before image tracking existed
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
                    .also { it.openHelper.writableDatabase } // Force migration
                } catch (e: Exception) {
                    android.util.Log.e("DiaryDatabase", "Migration failed, recreating database", e)
                    // Schema validation or migration failed — recreate from scratch
                    Room.databaseBuilder(
                        context.applicationContext,
                        DiaryDatabase::class.java,
                        "diary_database"
                    ).addMigrations(*allMigrations)
                    .fallbackToDestructiveMigration()
                    .addCallback(callback)
                    .build()
                    .also { it.openHelper.writableDatabase }
                }
                INSTANCE = instance
                instance
            }
        }

        /**
         * One-time backfill: populate diary_images for entries that were saved
         * before the image tracking feature existed. Uses raw SQL to extract
         * media references from content without depending on Kotlin helpers.
         */
        private fun backfillDiaryImages(db: SupportSQLiteDatabase, context: Context) {
            // Only run if diary_images is empty (first launch after upgrade)
            val countCursor = db.query("SELECT COUNT(*) FROM diary_images")
            val imageCount = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
            countCursor.close()
            if (imageCount > 0) return

            val mediaDir = java.io.File(context.filesDir, "diary_media")
            val thumbDir = java.io.File(mediaDir, "thumbs")

            // Read all entries with content
            val cursor = db.query("SELECT id, content FROM diary_entries")
            val idIdx = cursor.getColumnIndex("id")
            val contentIdx = cursor.getColumnIndex("content")

            val now = System.currentTimeMillis()
            var backfillCount = 0

            while (cursor.moveToNext()) {
                val entryId = cursor.getLong(idIdx)
                val content = cursor.getString(contentIdx) ?: continue
                if (content.isBlank()) continue

                // Extract media names using the same regexes as DiaryMediaManager
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
            cursor.close()

            if (backfillCount > 0) {
                android.util.Log.d("DiaryDatabase", "Backfilled $backfillCount diary_images for existing entries")
            }
        }
    }
}
