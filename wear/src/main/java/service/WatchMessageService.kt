package service

import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.sonyheadphonesremote.common.Constants
import com.example.sonyheadphonesremote.BaseApplication
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WatchMessageService: WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received on watch: ${messageEvent.path}")

        when (messageEvent.path) {
            Constants.PHONE_TO_WATCH_REPLY_PATH -> {
                val receivedMessage = String(messageEvent.data)
                Log.d(TAG, "Received message from phone: '$receivedMessage' (from node: ${messageEvent.sourceNodeId})")

                val updateUiIntent = Intent("com.example.sonyheadphonesremote.PHONE_MESSAGE_RECEIVED")
                updateUiIntent.putExtra("message", receivedMessage)
                sendBroadcast(updateUiIntent)
            }
            else -> {
                Log.d(TAG, "Unknown message path on phone: ${messageEvent.path}")
            }
        }
        stopSelf() // Stop the service immediately after handling the message
    }

    companion object {
        private const val TAG = "utf8coding WatchMessageService: "
    }
}