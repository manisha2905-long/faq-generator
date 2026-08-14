package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faqs")
data class FaqEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String,
    val summary: String,
    val depthLabel: String,
    val jsonContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
