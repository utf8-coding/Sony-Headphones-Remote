package com.example.sonyheadphonesremote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.sonyheadphonesremote.cpp.wrapper.HelloWorldWrapper

class DriverTestService : Service() {

    private val TAG = "DriverTestService"
    private val CHANNEL_ID = "Ordinary Channel"
    private val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    init {
        System.loadLibrary("sonyheadphonesremote")
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MyAdbService onStartCommand()")
        createNotificationChannel()
        val stopSelfIntent = Intent(this, DriverTestService::class.java)
        stopSelfIntent.action = ACTION_STOP_SERVICE
        val pStopSelf = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ADB Service Running")
            .setContentText("This service was started via ADB and is performing a task.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 使用一个系统图标作为示例
            .setTicker("Service started")
            .addAction(R.drawable.ic_launcher_foreground, "Stop Service", pStopSelf) // 添加停止按钮
            .build()
        startForeground(1, notification) // 1 是通知的唯一ID

        val action = intent?.action
        // Check Intent for stopping
        if (ACTION_STOP_SERVICE == action) {
            Log.d(TAG, "Stopping service via notification action.")
            stopSelf()
            return START_NOT_STICKY
        }

        //JNI test
        HelloWorldWrapper()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MyAdbService onDestroy()")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "ADB Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}