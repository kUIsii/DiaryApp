package com.diary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntry::class, Tag::class, DiaryTag::class, TodoItem::class, TrashEntry::class],
    version = 9,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE diary_entries ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
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
            override fun migrate(database: SupportSQLiteDatabase) {
                // Indices for diary_entries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_createdAt ON diary_entries (createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_isFavorite ON diary_entries (isFavorite)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_moodLevel ON diary_entries (moodLevel)")
                // Indices for diary_tag_cross_ref
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tag_cross_ref_tagId ON diary_tag_cross_ref (tagId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tag_cross_ref_diaryId ON diary_tag_cross_ref (diaryId)")
                // Indices for todo_items
                database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_dueDate ON todo_items (dueDate)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_isCompleted ON todo_items (isCompleted)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE todo_items ADD COLUMN category TEXT NOT NULL DEFAULT 'task'")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_category ON todo_items (category)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE todo_items ADD COLUMN reminderTime INTEGER")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_reminderTime ON todo_items (reminderTime)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
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
                database.execSQL("CREATE INDEX IF NOT EXISTS index_trash_entries_deletedAt ON trash_entries (deletedAt)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to todo_items
                database.execSQL("ALTER TABLE todo_items ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE todo_items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE todo_items ADD COLUMN parentId INTEGER")
                database.execSQL("ALTER TABLE todo_items ADD COLUMN recurringType TEXT NOT NULL DEFAULT 'none'")
                database.execSQL("ALTER TABLE todo_items ADD COLUMN progress INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE todo_items ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                // Create indices for new columns
                database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_parentId ON todo_items (parentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_tags ON todo_items (tags)")
            }
        }

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary_database"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
