package com.easyhome.senior

import android.app.Application
import com.easyhome.senior.di.AppContainer
import com.easyhome.senior.system.AppForegroundTracker

class EasyHomeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppForegroundTracker.install()
    }
}
