package com.example

import android.app.Application
import com.example.service.AppForegroundTracker

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppForegroundTracker.attach()
    }
}
