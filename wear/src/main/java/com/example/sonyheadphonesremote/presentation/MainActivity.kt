/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.sonyheadphonesremote.presentation

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContentValues.TAG
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.sonyheadphonesremote.BaseApplication
import com.example.sonyheadphonesremote.common.Constants
import com.example.sonyheadphonesremote.presentation.theme.SonyHeadphonesRemoteTheme
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HeadphoneState(val status: Status)

enum class Status {
    ANC, OFF, AMB, DISCONNECTED
}

class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main) // UI操作用Main Dispatcher
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        var headphoneState = mutableStateOf(HeadphoneState(Status.DISCONNECTED))

        // Handle intent from phone
        intent?.let {
            Log.d(TAG, "Received intent from phone: ${it.action}")
            val statusString = it.getStringExtra("HEADPHONE_STATUS")
            statusString?.let { str ->
                when(str.toInt()){
                    1 -> headphoneState.value = HeadphoneState(Status.OFF)
                    2 -> headphoneState.value = HeadphoneState(Status.ANC)
                    3 -> headphoneState.value = HeadphoneState(Status.AMB)
                    else -> headphoneState.value = HeadphoneState(Status.DISCONNECTED)
                }
            }
        }

        val filter = IntentFilter("PHONE_MESSAGE_RECEIVED")
        messageReceiver.onStateReceived = { newState ->
            headphoneState.value = newState
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, filter)

        setContent {
            WearApp(headphoneState) { state ->
                sendMessageToPhone("$state", Constants.WATCH_TO_PHONE_MESSAGE_PATH)
            }
        }
    }

    private fun sendMessageToPhone(message: String, path: String) {
        activityScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                Log.d(TAG, "Connected phone nodes: ${nodes.size}")

                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected phone found.")
                    Toast.makeText(BaseApplication.getContext(), "nodes empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                for (node in nodes) {
                    val sendResult = messageClient.sendMessage(
                        node.id,
                        path,
                        message.toByteArray()
                    ).await()
                    Log.d(TAG, "Message sent to phone (${node.displayName}): $sendResult")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to phone: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        activityScope.cancel()
    }

    // BroadcastReceiver to handle messages from MessageReceiverService
    private val messageReceiver = object : BroadcastReceiver() {
        var onStateReceived: (HeadphoneState) -> Unit = { newState ->  }

        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            intent?.let {
                val statusString = it.getStringExtra("HEADPHONE_STATUS")
                statusString?.let { str ->
                    when(str.toInt()){
                        1 -> onStateReceived(HeadphoneState(Status.OFF))
                        2 -> onStateReceived(HeadphoneState(Status.ANC))
                        3 -> onStateReceived(HeadphoneState(Status.AMB))
                        else -> onStateReceived(HeadphoneState(Status.DISCONNECTED))
                    }
                }
            }
        }
    }
}

@Composable
fun WearApp(
    headphoneState: MutableState<HeadphoneState>,
    onChangeState: (Int) -> Unit
) {
    SonyHeadphonesRemoteTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        ) {
            TimeText()
            // You can use headphoneState.status here to update the UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Add padding for overall layout
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), // Uniform spacing
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusIndicator(isConnected = headphoneState.value.status != Status.DISCONNECTED)

                Button(
                    modifier = Modifier
                        .width(100.dp)
                        .height(35.dp),
                    onClick = {
                        onChangeState(1)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "Custom")
                }
                Button(
                    modifier = Modifier
                        .width(100.dp)
                        .height(35.dp),
                    onClick = {
                        onChangeState(2)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "NC")
                }
                Button(
                    modifier = Modifier
                        .width(100.dp)
                        .height(35.dp),
                    onClick = {
                        onChangeState(3)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "Ambient")
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(isConnected: Boolean) {
    val iconText = if (isConnected) "✔" else "✘"
    val iconColor = if (isConnected) Color.Green else Color.Red
    Text(text = iconText, color = iconColor)
}

@Preview(showBackground = true)
@Composable
fun ConnectedStatusPreview() {
    StatusIndicator(isConnected = true)
}

@Preview(showBackground = true)
@Composable
fun DisconnectedStatusPreview() {
    StatusIndicator(isConnected = false)
}
