package com.example.sonyheadphonesremote

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class BaseApplication: Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context

        fun getContext(): Context {
            return context
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}