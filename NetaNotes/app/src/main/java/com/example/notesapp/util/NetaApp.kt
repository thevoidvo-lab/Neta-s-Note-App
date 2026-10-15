package com.example.notesapp.util

import android.app.Application

class NetaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
