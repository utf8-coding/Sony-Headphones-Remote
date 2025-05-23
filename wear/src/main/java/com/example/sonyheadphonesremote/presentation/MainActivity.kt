/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.sonyheadphonesremote.presentation

import android.content.ContentValues.TAG
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.sonyheadphonesremote.BaseApplication
import com.example.sonyheadphonesremote.common.Constants
import com.example.sonyheadphonesremote.presentation.theme.SonyHeadphonesRemoteTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HeadphoneState(val status: Status)

enum class Status {
    ANC, OFF, ENV, DISCONNECTED
}

class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main) // UI操作用Main Dispatcher
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        var headphoneState by mutableStateOf(HeadphoneState(Status.DISCONNECTED))

        // Handle intent from phone
        intent?.let {
            val statusString = it.getStringExtra("HEADPHONE_STATUS")
            statusString?.let { str ->
                val newStatus = Status.valueOf(str.uppercase()) // Ensure uppercase to match enum
                headphoneState = HeadphoneState(newStatus)
                Log.d(TAG, "Received headphone status from phone: $newStatus")
            }
        }

        setContent {
            WearApp(headphoneState) { state ->
                when(state){
                    1 -> { // Example: This could be tied to a button press now
                        sendMessageToPhone("state1", Constants.WATCH_TO_PHONE_MESSAGE_PATH)
                    }
                }
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
        activityScope.cancel() // 取消所有协程
    }
}

@Composable
fun WearApp(
    headphoneState: HeadphoneState, // Pass the state to the Composable
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
                Text(text = "Status: ${headphoneState.status}") // Display the current status
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
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "NC")
                }
                Button(
                    modifier = Modifier
                        .width(100.dp)
                        .height(35.dp),
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "Ambient")
                }
            }
        }
    }
}