package com.matrixconnect.data.dao

import androidx.room.*
import com.matrixconnect.data.entities.ServerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {
    @Query("SELECT * FROM server_configs ORDER BY name ASC")
    fun getAllServerConfigs(): List<ServerConfig>

    @Query("SELECT * FROM server_configs ORDER BY name ASC")
    fun observeAllServerConfigs(): Flow<List<ServerConfig>>

    @Query("SELECT * FROM server_configs WHERE id = :id")
    suspend fun getServerConfigById(id: Long): ServerConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(serverConfig: ServerConfig): Long

    @Update
    suspend fun update(serverConfig: ServerConfig)

    @Delete
    suspend fun delete(serverConfig: ServerConfig)

    @Query("UPDATE server_configs SET lastConnected = :timestamp WHERE id = :serverId")
    suspend fun updateLastConnected(serverId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM server_configs WHERE isAutoConnect = 1 LIMIT 1")
    suspend fun getAutoConnectServer(): ServerConfig?

    @Query("SELECT COUNT(*) FROM server_configs")
    suspend fun getServerCount(): Int
}
