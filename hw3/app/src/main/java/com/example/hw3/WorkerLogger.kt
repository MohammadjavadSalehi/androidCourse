package com.example.hw3

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class StatusCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private fun resetWorkerTimer() {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
        val requestChecking =
            OneTimeWorkRequestBuilder<StatusCheckWorker>().setConstraints(constraints)
                .setInitialDelay(120, TimeUnit.SECONDS).build()
        WorkManager.getInstance(applicationContext).enqueue(requestChecking)
    }

    private fun checkAirplaneMode() {
        val isAirplaneOn = Settings.Global.getInt(
            applicationContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
        Log.i("worker_airplane", "Airplane mode is ${if (isAirplaneOn) "On" else "Off"}")
    }

    private fun checkBluetoothMode() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothOn = bluetoothAdapter?.isEnabled ?: false
        Log.i(
            "worker_bluetooth", "Bluetooth is ${if (isBluetoothOn) "Enabled" else "Disabled"}"
        )
    }

    override fun doWork(): Result {
        checkAirplaneMode()
        checkBluetoothMode()
        resetWorkerTimer()
        return Result.success()
    }
}

fun startWorker(context: Context) {
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
    val requestChecking =
        OneTimeWorkRequestBuilder<StatusCheckWorker>().setConstraints(constraints).build()
    WorkManager.getInstance(context).enqueue(requestChecking)
}