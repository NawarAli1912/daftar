package com.daftar.app

import android.app.Application
import com.daftar.app.reminders.RemindersWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DaftarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RemindersWorker.ensureChannel(this)
        RemindersWorker.scheduleDaily(this) // once-a-day debt digest (NFR-5)
    }
}
