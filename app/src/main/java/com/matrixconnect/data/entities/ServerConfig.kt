package com.matrixconnect.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_configs")
data class ServerConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val encryptionType: String,
    val encryptionKey: String,
    val isAutoConnect: Boolean = false,
    val useKillSwitch: Boolean = false,
    val useStealth: Boolean = false,
    val notes: String? = null,
    val lastConnected: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
