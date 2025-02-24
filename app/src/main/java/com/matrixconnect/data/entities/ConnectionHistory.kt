package com.matrixconnect.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "connection_history",
    foreignKeys = [
        ForeignKey(
            entity = ServerConfig::class,
            parentColumns = ["id"],
            childColumns = ["serverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serverId")]
)
data class ConnectionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long,
    val startTime: Long,
    var endTime: Long? = null,
    val protocol: String,
    val encryptionType: String,
    var bytesReceived: Long = 0,
    var bytesSent: Long = 0,
    var connectionStatus: String,
    var disconnectReason: String? = null,
    var errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    val isActive: Boolean
        get() = endTime == null && connectionStatus == "CONNECTED"
}
