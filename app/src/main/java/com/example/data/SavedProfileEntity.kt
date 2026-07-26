package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_profiles")
data class SavedProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val widthDp: Int,
    val heightDp: Int,
    val offsetXDp: Int,
    val offsetYDp: Int,
    val cornerRadiusDp: Int,
    val backgroundColorHex: String,
    val backgroundAlpha: Float,
    val autoCollapseSeconds: Int,
    val vibrationFeedback: Boolean,
    val enabledModules: String, // comma-joined Set<String>
    val islandShape: String = "DOT_EXPAND"
)
