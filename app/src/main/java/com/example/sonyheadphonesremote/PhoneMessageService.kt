package com.example.sonyheadphonesremote

import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.sonyheadphonesremote.common.Constants
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneMessageService: WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received on phone: ${messageEvent.path}")

        when (messageEvent.path) {
            Constants.WATCH_TO_PHONE_MESSAGE_PATH -> {
                val receivedState = String(messageEvent.data).toInt()
                Log.d(TAG, "Received from watch: '$receivedState' (from node: ${messageEvent.sourceNodeId})")

                //TODO: TEMP reply, should base on the actual state
                val replyMessage = "$receivedState"
                sendReplyToWatch(replyMessage, Constants.PHONE_TO_WATCH_REPLY_PATH, messageEvent.sourceNodeId)
            }
            else -> {
                Log.d(TAG, "Unknown message path on phone: ${messageEvent.path}")
            }
        }
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

    companion object {
        private const val TAG = "utf8coding PhoneMessageService: "
    }
}