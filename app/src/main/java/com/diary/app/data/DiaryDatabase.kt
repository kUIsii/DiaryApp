package com.diary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntry::class, Tag::class, DiaryTag::class, TodoItem::class, TrashEntry::class, DiaryImage::class, CountDownItem::class, HabitRecord::class],
    version = 14,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

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

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try { db.execSQL("DROP INDEX IF EXISTS index_countdown_items_targetDate") } catch (_: Exception) {}
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
