package com.example.hackverse

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MediaManager.init(this, hashMapOf(
            "cloud_name" to "dcfjykkek",
            "api_key" to "571655651469164",
            "api_secret" to "hbL-NcjtC87u98k9SWx3C0X9OvQ"
        ))
    }
}