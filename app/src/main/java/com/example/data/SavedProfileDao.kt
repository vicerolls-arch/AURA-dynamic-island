package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedProfileDao {
    @Query("SELECT * FROM saved_profiles")
    fun getAllProfiles(): Flow<List<SavedProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SavedProfileEntity)

    @Query("DELETE FROM saved_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM saved_profiles")
    suspend fun getCount(): Int
}
