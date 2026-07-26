package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alert_history ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertHistoryEntity)

    @Query("DELETE FROM alert_history WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM alert_history")
    suspend fun clearAll()
}
