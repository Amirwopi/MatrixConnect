package com.matrixconnect.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixconnect.data.AppDatabase
import com.matrixconnect.data.entities.ConnectionHistory
import com.matrixconnect.data.entities.ServerConfig
import com.matrixconnect.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class ConnectionStats(
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val uptime: Long = 0,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class MainViewModel : ViewModel() {
    private val _selectedServer = MutableLiveData<ServerConfig?>()
    val selectedServer: LiveData<ServerConfig?> = _selectedServer

    private val _connectionStats = MutableStateFlow(ConnectionStats())
    val connectionStats: StateFlow<ConnectionStats> = _connectionStats

    private val _connectionHistory = MutableStateFlow<List<ConnectionHistory>>(emptyList())
    val connectionHistory: StateFlow<List<ConnectionHistory>> = _connectionHistory

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var startTime: Long = 0

    init {
        loadLastServer()
        startStatsUpdates()
    }

    private fun loadLastServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val serverId = PreferenceManager.getInstance().selectedServer?.toLongOrNull()
            if (serverId != null) {
                _selectedServer.postValue(
                    AppDatabase.getInstance().serverConfigDao().getServerConfigById(serverId)
                )
            }
        }
    }

    fun selectServer(server: ServerConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            PreferenceManager.getInstance().selectedServer = server.id.toString()
            _selectedServer.postValue(server)
        }
    }

    fun updateConnectionStatus(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.CONNECTED -> startTime = System.currentTimeMillis()
            ConnectionStatus.DISCONNECTED -> startTime = 0
            else -> {}
        }
        _connectionStats.value = _connectionStats.value.copy(status = status)
    }

    fun updateTransferStats(bytesReceived: Long, bytesSent: Long) {
        _connectionStats.value = _connectionStats.value.copy(
            bytesReceived = bytesReceived,
            bytesSent = bytesSent,
            uptime = if (startTime > 0) System.currentTimeMillis() - startTime else 0
        )
    }

    fun loadConnectionHistory(serverId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _connectionHistory.value = AppDatabase.getInstance()
                .connectionHistoryDao()
                .getConnectionHistoryByServerId(serverId)
        }
    }

    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return DecimalFormat("#,##0.##").format(value) + " " + units[unitIndex]
    }

    fun formatUptime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startStatsUpdates() {
        viewModelScope.launch(Dispatchers.Default) {
            // Update uptime every second when connected
            while (true) {
                if (_connectionStats.value.status == ConnectionStatus.CONNECTED) {
                    _connectionStats.value = _connectionStats.value.copy(
                        uptime = System.currentTimeMillis() - startTime
                    )
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}
