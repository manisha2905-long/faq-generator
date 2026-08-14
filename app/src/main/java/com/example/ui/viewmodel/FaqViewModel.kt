package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FaqDepth
import com.example.data.model.FaqResponse
import com.example.data.repository.FaqRepository
import com.example.data.repository.SavedFaqItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface FaqUiState {
    object Idle : FaqUiState
    data class Loading(val message: String = "Analyzing topic & generating structured FAQ...") : FaqUiState
    data class Success(val faqResponse: FaqResponse, val dbId: Long? = null, val isSaved: Boolean = false) : FaqUiState
    data class Error(val message: String) : FaqUiState
}

class FaqViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FaqRepository
    private var textToSpeech: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)

    init {
        val database = AppDatabase.getInstance(application)
        repository = FaqRepository(database.faqDao())

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                _isTtsReady.value = true
            }
        }

        // Collect saved FAQs
        viewModelScope.launch {
            repository.allSavedFaqs.collectLatest { list ->
                _savedFaqs.value = list
            }
        }
    }

    private val _topicInput = MutableStateFlow("")
    val topicInput: StateFlow<String> = _topicInput.asStateFlow()

    private val _selectedDepth = MutableStateFlow(FaqDepth.BEGINNER)
    val selectedDepth: StateFlow<FaqDepth> = _selectedDepth.asStateFlow()

    private val _uiState = MutableStateFlow<FaqUiState>(FaqUiState.Idle)
    val uiState: StateFlow<FaqUiState> = _uiState.asStateFlow()

    private val _savedFaqs = MutableStateFlow<List<SavedFaqItem>>(emptyList())
    val savedFaqs: StateFlow<List<SavedFaqItem>> = _savedFaqs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSectionFilter = MutableStateFlow("All")
    val selectedSectionFilter: StateFlow<String> = _selectedSectionFilter.asStateFlow()

    private val _speakingText = MutableStateFlow<String?>(null)
    val speakingText: StateFlow<String?> = _speakingText.asStateFlow()

    fun onTopicInputChanged(newText: String) {
        _topicInput.value = newText
    }

    fun onDepthSelected(depth: FaqDepth) {
        _selectedDepth.value = depth
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSectionFilterChanged(section: String) {
        _selectedSectionFilter.value = section
    }

    fun clearInput() {
        _topicInput.value = ""
    }

    fun generateFaq() {
        val topic = _topicInput.value.trim()
        if (topic.isBlank()) return

        viewModelScope.launch {
            _uiState.value = FaqUiState.Loading("Analyzing '${topic.take(30)}'...")
            try {
                val response = repository.generateFaq(topic, _selectedDepth.value)
                // Automatically save generated FAQ to Room DB so it's kept in history
                val dbId = repository.saveFaqToDatabase(response, _selectedDepth.value.label)
                _uiState.value = FaqUiState.Success(faqResponse = response, dbId = dbId, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = FaqUiState.Error(e.localizedMessage ?: "Failed to generate FAQ. Please try again.")
            }
        }
    }

    fun selectSampleTopic(sample: String) {
        _topicInput.value = sample
        generateFaq()
    }

    fun loadSavedFaq(savedFaq: SavedFaqItem) {
        _uiState.value = FaqUiState.Success(
            faqResponse = savedFaq.faqResponse,
            dbId = savedFaq.id,
            isSaved = true
        )
    }

    fun toggleFavorite(savedFaq: SavedFaqItem) {
        viewModelScope.launch {
            repository.toggleFavorite(savedFaq.id, !savedFaq.isFavorite)
        }
    }

    fun deleteSavedFaq(id: Long) {
        viewModelScope.launch {
            repository.deleteFaq(id)
            val currentState = _uiState.value
            if (currentState is FaqUiState.Success && currentState.dbId == id) {
                _uiState.value = FaqUiState.Idle
            }
        }
    }

    fun speakText(text: String) {
        if (_speakingText.value == text) {
            stopSpeaking()
            return
        }

        if (_isTtsReady.value) {
            textToSpeech?.stop()
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FAQ_TTS")
            _speakingText.value = text
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _speakingText.value = null
    }

    fun resetToInput() {
        stopSpeaking()
        _uiState.value = FaqUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
