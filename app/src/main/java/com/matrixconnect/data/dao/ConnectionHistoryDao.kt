package com.matrixconnect.data.dao

import androidx.room.*
import com.matrixconnect.data.entities.ConnectionHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionHistoryDao {
    @Query("SELECT * FROM connection_history ORDER BY startTime DESC")
    fun getAllConnectionHistory(): List<ConnectionHistory>

    @Query("SELECT * FROM connection_history WHERE serverId = :serverId ORDER BY startTime DESC")
    fun getConnectionHistoryByServerId(serverId: Long): List<ConnectionHistory>

    @Query("SELECT * FROM connection_history ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastConnection(): ConnectionHistory?

    @Query("SELECT * FROM connection_history WHERE connectionStatus = 'CONNECTED' AND endTime IS NULL")
    suspend fun getActiveConnection(): ConnectionHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connectionHistory: ConnectionHistory): Long

    @Update
    suspend fun update(connectionHistory: ConnectionHistory)

    @Delete
    suspend fun delete(connectionHistory: ConnectionHistory)

    @Query("DELETE FROM connection_history WHERE serverId = :serverId")
    suspend fun deleteAllForServer(serverId: Long)

    @Query("""
        SELECT * FROM connection_history 
        WHERE startTime >= :startTime 
        AND startTime <= :endTime 
        ORDER BY startTime DESC
    """)
    fun getConnectionHistoryBetween(startTime: Long, endTime: Long): List<ConnectionHistory>

    @Query("""
        SELECT SUM(bytesReceived) 
        FROM connection_history 
        WHERE startTime >= :startTime 
        AND startTime <= :endTime
    """)
    suspend fun getTotalBytesReceivedBetween(startTime: Long, endTime: Long): Long?

    @Query("""
        SELECT SUM(bytesSent) 
        FROM connection_history 
        WHERE startTime >= :startTime 
        AND startTime <= :endTime
    """)
    suspend fun getTotalBytesSentBetween(startTime: Long, endTime: Long): Long?

    @Query("""
        SELECT * FROM connection_history 
        WHERE connectionStatus = 'ERROR' 
        ORDER BY startTime DESC 
        LIMIT :limit
    """)
    fun getRecentErrors(limit: Int = 10): List<ConnectionHistory>

    @Query("SELECT COUNT(*) FROM connection_history WHERE serverId = :serverId")
    suspend fun getConnectionCountForServer(serverId: Long): Int

    @Transaction
    suspend fun endActiveConnection(endTime: Long = System.currentTimeMillis()) {
        getActiveConnection()?.let { connection ->
            connection.endTime = endTime
            update(connection)
        }
    }
}
