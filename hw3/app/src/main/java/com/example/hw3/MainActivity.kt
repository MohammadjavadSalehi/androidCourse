package com.example.hw3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.annotation.RequiresApi
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.FileObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.io.File


class MainActivity : ComponentActivity() {

    private var bluetoothStatus by mutableStateOf("Disconnected")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startWorker(this)
        val permissionState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

        // request permission
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        } else {
            // create the notification channel
            createNotificationChannel()
        }

        setContent {
            println("status $bluetoothStatus")
            displayBluetoothStatus(bluetoothStatus)
            LogDisplayScreen(this)
        }

        val bluetoothStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getStringExtra(BluetoothLogger.EXTRA_STATUS) ?: "Disconnected"
                bluetoothStatus = status
            }
        }

        val filter = IntentFilter(BluetoothLogger.ACTION_BLUETOOTH_STATUS)
        registerReceiver(bluetoothStatusReceiver, filter, RECEIVER_NOT_EXPORTED)

        lifecycleScope.launch {
            startService(Intent(this@MainActivity, BluetoothLogger::class.java))
        }
    }

    private val bluetoothStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra(BluetoothLogger.EXTRA_STATUS) ?: "Disconnected"
            bluetoothStatus = status
        }

    }

    private fun createNotificationChannel() {
        // Create a notification channel
        val channelId = "bluetooth_channel"
        val channelName = "Bluetooth Status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStatusReceiver)
    }

}

fun readFiles(context: Context): List<String> {
    val logFile = File(context.filesDir, "logs.txt")
    var logs = mutableListOf<String>()
    if (logFile.exists()) {
        logs = logFile.readLines().toMutableList();
    }
    return logs.reversed()
}

class LogFileWatcher(
    private val context: Context, private val callback: () -> Unit
) : FileObserver(context.filesDir.path + "logs.txt", CREATE or MODIFY) {

    override fun onEvent(event: Int, path: String?) {
        if (event == CREATE || event == MODIFY) {
            callback()
        }
    }
}


@Composable
fun displayBluetoothStatus(status: String) {
    val backgroundColor = if (status == "Connected") Color.Green else Color.Gray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bluetooth Status",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Bluetooth $status", modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun LogDisplayScreen(context: Context) {
    val logs = remember { mutableStateOf(readFiles(context)) }

    val observer = remember {
        LogFileWatcher(context) {
            logs.value = readFiles(context)
        }
    }

    DisposableEffect(context) {
        observer.startWatching()
        onDispose {
            observer.stopWatching()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        items(logs.value) { log ->
            Text(text = log,modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            Divider(color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    displayBluetoothStatus("Disconnected")
}