package com.diary.app.data.ambientsound

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, RecentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AmbientSoundDatabase : RoomDatabase() {
    abstract fun dao(): AmbientSoundDao

    companion object {
        @Volatile
        private var INSTANCE: AmbientSoundDatabase? = null

        fun getInstance(context: Context): AmbientSoundDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AmbientSoundDatabase::class.java,
                    "ambient_sound.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
