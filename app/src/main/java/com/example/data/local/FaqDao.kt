package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaqDao {
    @Query("SELECT * FROM faqs ORDER BY timestamp DESC")
    fun getAllFaqs(): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteFaqs(): Flow<List<FaqEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaq(faq: FaqEntity): Long

    @Query("UPDATE faqs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM faqs WHERE id = :id")
    suspend fun deleteFaqById(id: Long)

    @Query("DELETE FROM faqs")
    suspend fun clearAll()
}
