package com.example.hw3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BluetoothLogger : Service() {
    private lateinit var notifChannelId: String
    private lateinit var notifManager: NotificationManagerCompat
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun displayBluetoothNotif(status: String) {
        createNotifChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, notifChannelId)
            .setContentTitle("Bluetooth Status Notification")
            .setContentText("Bluetooth $status").setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent).setTimeoutAfter(10000).build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notifManager.notify(1, notification)
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status =
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> "Connected"
                    BluetoothAdapter.STATE_OFF -> "Disconnected"
                    else -> "Unknown"
                }
            println("state: $status")

            val broadcastIntent: Intent = Intent(ACTION_BLUETOOTH_STATUS)
            broadcastIntent.putExtra(EXTRA_STATUS, status)
            sendBroadcast(broadcastIntent)
            displayBluetoothNotif(status)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

    }

    private fun createNotifChannel() {
        notifManager = NotificationManagerCompat.from(this)
        notifChannelId = "bluetooth_channel"
        val channelName = "Bluetooth Status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(notifChannelId, channelName, importance)
        notifManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val bluetoothAdapter: BluetoothAdapter =
            BluetoothAdapter.getDefaultAdapter() ?: return START_NOT_STICKY

        val isEnabled = bluetoothAdapter.isEnabled
        val status = if (!isEnabled) "Disconnected" else "Connected"

        val broadcastIntent = Intent(ACTION_BLUETOOTH_STATUS)
        broadcastIntent.putExtra(EXTRA_STATUS, status)
        sendBroadcast(broadcastIntent)

        displayBluetoothNotif(status)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
    }

    companion object {
        const val ACTION_BLUETOOTH_STATUS = "action.BLUETOOTH_STATUS"
        const val EXTRA_STATUS = "extra.STATUS"
    }
}