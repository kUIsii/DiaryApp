package com.diary.app.di

import android.content.Context
import com.diary.app.data.DiaryDao
import com.diary.app.data.DiaryDatabase

/**
 * Simple Service Locator for dependency injection.
 * Provides centralized access to database and DAO instances.
 */
class AppContainer(context: Context) {
    val database: DiaryDatabase = DiaryDatabase.getDatabase(context)
    val dao: DiaryDao = database.diaryDao()
}
