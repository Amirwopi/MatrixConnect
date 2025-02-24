package com.matrixconnect.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.matrixconnect.MatrixConnectApp
import com.matrixconnect.R
import com.matrixconnect.activities.MainActivity
import com.matrixconnect.data.AppDatabase
import com.matrixconnect.data.entities.ConnectionHistory
import com.matrixconnect.data.entities.ServerConfig
import com.matrixconnect.network.ProxyConnection
import com.matrixconnect.utils.PreferenceManager
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class ConnectionService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var currentConnection: ProxyConnection? = null
    private var currentServerConfig: ServerConfig? = null
    private var connectionHistory: ConnectionHistory? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.matrixconnect.action.START"
        const val ACTION_STOP = "com.matrixconnect.action.STOP"
        const val EXTRA_SERVER_ID = "server_id"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("Initializing..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val serverId = intent.getLongExtra(EXTRA_SERVER_ID, -1)
                if (serverId != -1L) {
                    serviceScope.launch {
                        startConnection(serverId)
                    }
                }
            }
            ACTION_STOP -> {
                stopConnection()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startConnection(serverId: Long) {
        try {
            val serverConfig = AppDatabase.getInstance().serverConfigDao()
                .getServerConfigById(serverId) ?: throw IllegalStateException("Server config not found")
            
            currentServerConfig = serverConfig
            connectionHistory = ConnectionHistory(
                serverId = serverId,
                startTime = System.currentTimeMillis(),
                protocol = serverConfig.protocol,
                encryptionType = serverConfig.encryptionType,
                connectionStatus = "CONNECTING"
            )

            updateNotification("Connecting to ${serverConfig.name}...")

            currentConnection = ProxyConnection(
                serverConfig = serverConfig,
                onBytesTransferred = { received, sent ->
                    connectionHistory?.let {
                        it.bytesReceived += received
                        it.bytesSent += sent
                    }
                },
                onError = { error ->
                    handleConnectionError(error)
                }
            )

            if (PreferenceManager.getInstance().killSwitch) {
                setupKillSwitch()
            }

            currentConnection?.start()
            
            connectionHistory?.connectionStatus = "CONNECTED"
            updateNotification("Connected to ${serverConfig.name}")
            
        } catch (e: Exception) {
            handleConnectionError(e)
        }
    }

    private fun stopConnection() {
        serviceScope.launch {
            try {
                currentConnection?.stop()
                connectionHistory?.let {
                    it.endTime = System.currentTimeMillis()
                    it.disconnectReason = "USER_INITIATED"
                    AppDatabase.getInstance().connectionHistoryDao().insert(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        currentConnection = null
        currentServerConfig = null
        connectionHistory = null
    }

    private fun handleConnectionError(error: Throwable) {
        serviceScope.launch {
            connectionHistory?.let {
                it.endTime = System.currentTimeMillis()
                it.connectionStatus = "FAILED"
                it.errorMessage = error.message
                it.disconnectReason = when (error) {
                    is IOException -> "NETWORK_ERROR"
                    else -> "UNKNOWN_ERROR"
                }
                AppDatabase.getInstance().connectionHistoryDao().insert(it)
            }
            updateNotification("Connection failed: ${error.message}")
            stopSelf()
        }
    }

    private fun setupKillSwitch() {
        serviceScope.launch {
            while (currentConnection?.isActive == true) {
                if (!isConnectionHealthy()) {
                    handleConnectionError(IOException("Kill switch activated: Connection unhealthy"))
                    break
                }
                delay(1000)
            }
        }
    }

    private fun isConnectionHealthy(): Boolean {
        return try {
            currentServerConfig?.let { config ->
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(config.host, config.port), 5000)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun createNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MatrixConnectApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MatrixConnect")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopConnection()
        serviceScope.cancel()
    }
}
