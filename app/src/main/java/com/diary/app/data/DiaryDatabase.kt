package com.diary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntry::class, Tag::class, DiaryTag::class, TodoItem::class],
    version = 5,
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

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary_database"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
