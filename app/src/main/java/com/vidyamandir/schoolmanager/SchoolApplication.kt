package com.vidyamandir.schoolmanager

import android.app.Application
import com.vidyamandir.schoolmanager.util.FirebaseSyncManager

class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseSyncManager.init(this)
    }
}
