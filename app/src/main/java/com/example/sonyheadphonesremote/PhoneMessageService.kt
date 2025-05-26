package com.example.sonyheadphonesremote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.sonyheadphonesremote.common.Constants
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PhoneMessageService: WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        // check permission
        if(checkAndRequestBluetoothPermissions()){
            stopSelf()
        }
        // check path
        if(messageEvent.path != Constants.WATCH_TO_PHONE_MESSAGE_PATH) {
            Log.w(TAG, "Unknown message path on phone: ${messageEvent.path}")
            stopSelf()
        }

        val receivedState = String(messageEvent.data).toIntOrNull()
        if (receivedState == null) {
            Log.w(TAG, "Received message is not a number indicating ANC state")
            Toast.makeText(this, "SonyHeadphonesRemote: Invalid message received.", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
        Log.d(TAG, "Received from watch: '$receivedState' (from node: ${messageEvent.sourceNodeId})")

        //TODO: TEMP reply, should base on the actual state
        val replyMessage = "$receivedState"
        sendReplyToWatch(replyMessage, Constants.PHONE_TO_WATCH_REPLY_PATH, messageEvent.sourceNodeId)

        stopSelf() // Stop the service immediately after handling the message
    }

    private fun sendReplyToWatch(message: String, path: String, targetNodeId: String) {
        serviceScope.launch {
            try {
                val messageClient = Wearable.getMessageClient(this@PhoneMessageService)
                val sendResult = messageClient.sendMessage(
                    targetNodeId,
                    path,
                    message.toByteArray()
                )
                Log.d(TAG, "Reply sent to watch ($targetNodeId): $sendResult")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send reply to watch: ${e.message}", e)
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions(): Boolean {
        val requiredPermissions = mutableListOf<String>()

        // Legacy Bluetooth permissions for older devices
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) { // R is API 30
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH)
            }
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // New Bluetooth permissions for Android 12 (API 31) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S is API 31
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (requiredPermissions.isNotEmpty()) {
            Log.w(TAG, "Bluetooth permissions are missing: $requiredPermissions. Please grant them in app settings.")
            // In a real app, you would request these permissions from an Activity.
            // For a WearableListenerService, logging is the primary way to indicate this.
            Toast.makeText(this, "SonyHeadphonesRemote: Bluetooth permissions required. Please grant them in app settings.", Toast.LENGTH_LONG).show()
            return false
        } else {
            Log.d(TAG, "All required Bluetooth permissions are granted.")
            return true
        }
    }

    companion object {
        private const val TAG = "utf8coding PhoneMessageService: "
    }
}