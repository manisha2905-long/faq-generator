package com.example.data.repository

import com.example.data.local.FaqDao
import com.example.data.local.FaqEntity
import com.example.data.model.FaqDepth
import com.example.data.model.FaqResponse
import com.example.data.remote.FaqGeneratorService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FaqRepository(
    private val faqDao: FaqDao,
    private val generatorService: FaqGeneratorService = FaqGeneratorService()
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val faqAdapter = moshi.adapter(FaqResponse::class.java)

    val allSavedFaqs: Flow<List<SavedFaqItem>> = faqDao.getAllFaqs().map { list ->
        list.mapNotNull { entity -> entity.toSavedFaqItem(faqAdapter) }
    }

    val favoriteFaqs: Flow<List<SavedFaqItem>> = faqDao.getFavoriteFaqs().map { list ->
        list.mapNotNull { entity -> entity.toSavedFaqItem(faqAdapter) }
    }

    suspend fun generateFaq(topic: String, depth: FaqDepth): FaqResponse {
        return generatorService.generateFaq(topic, depth)
    }

    suspend fun saveFaqToDatabase(faqResponse: FaqResponse, depthLabel: String): Long {
        val jsonString = faqAdapter.toJson(faqResponse)
        val entity = FaqEntity(
            topic = faqResponse.topic,
            summary = faqResponse.summary,
            depthLabel = depthLabel,
            jsonContent = jsonString
        )
        return faqDao.insertFaq(entity)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        faqDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteFaq(id: Long) {
        faqDao.deleteFaqById(id)
    }

    private fun FaqEntity.toSavedFaqItem(adapter: com.squareup.moshi.JsonAdapter<FaqResponse>): SavedFaqItem? {
        val parsed = try {
            adapter.fromJson(jsonContent)
        } catch (e: Exception) {
            null
        } ?: return null

        return SavedFaqItem(
            id = id,
            topic = topic,
            summary = summary,
            depthLabel = depthLabel,
            faqResponse = parsed,
            timestamp = timestamp,
            isFavorite = isFavorite
        )
    }
}

data class SavedFaqItem(
    val id: Long,
    val topic: String,
    val summary: String,
    val depthLabel: String,
    val faqResponse: FaqResponse,
    val timestamp: Long,
    val isFavorite: Boolean
)
