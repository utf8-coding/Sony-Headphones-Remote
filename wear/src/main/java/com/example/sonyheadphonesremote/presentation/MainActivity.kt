/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.sonyheadphonesremote.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.example.sonyheadphonesremote.R
import com.example.sonyheadphonesremote.presentation.theme.SonyHeadphonesRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp("Android")
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    SonyHeadphonesRemoteTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        ) {
            TimeText()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Add padding for overall layout
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), // Uniform spacing
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier.width(100.dp).height(35.dp),
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "1")
                }
                Button(
                    modifier = Modifier.width(100.dp).height(35.dp),
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "2")
                }
                Button(
                    modifier = Modifier.width(100.dp).height(35.dp),
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text(text = "3")
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}