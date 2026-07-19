package com.daftar.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DaftarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.daftar.app.sync.SyncWorker.schedule(this) // opportunistic backup push (no-op until a URL is set)
    }
}
