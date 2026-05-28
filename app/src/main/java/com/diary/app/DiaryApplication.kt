package com.diary.app

import android.app.Application
import com.diary.app.data.DiaryDatabase

class DiaryApplication : Application() {
    val database by lazy { DiaryDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
