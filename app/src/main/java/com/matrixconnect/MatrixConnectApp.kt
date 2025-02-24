package com.matrixconnect

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.matrixconnect.data.AppDatabase
import com.matrixconnect.utils.PreferenceManager

class MatrixConnectApp : Application(), Configuration.Provider {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "matrix_connect_service"
        lateinit var instance: MatrixConnectApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize components
        PreferenceManager.init(this)
        AppDatabase.init(this)
        initNotificationChannel()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
