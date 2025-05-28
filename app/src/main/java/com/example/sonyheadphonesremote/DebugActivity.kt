package com.example.sonyheadphonesremote

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DebugActivity : AppCompatActivity() {
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val svcIntent = Intent(this, DriverTestService::class.java)
        startForegroundService(svcIntent)

        finish()
    }
}
