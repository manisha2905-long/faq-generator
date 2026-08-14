package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FaqDisplayView
import com.example.ui.components.FaqHeader
import com.example.ui.components.FaqHistorySheet
import com.example.ui.components.TopicInputSection
import com.example.ui.viewmodel.FaqUiState
import com.example.ui.viewmodel.FaqViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    viewModel: FaqViewModel,
    modifier: Modifier = Modifier
) {
    val topicInput by viewModel.topicInput.collectAsStateWithLifecycle()
    val selectedDepth by viewModel.selectedDepth.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedFaqs by viewModel.savedFaqs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedSectionFilter.collectAsStateWithLifecycle()
    val speakingText by viewModel.speakingText.collectAsStateWithLifecycle()

    var isHistoryOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            FaqHeader(
                historyCount = savedFaqs.size,
                onOpenHistory = { isHistoryOpen = true },
                onNewFaq = { viewModel.resetToInput() },
                showNewButton = uiState is FaqUiState.Success
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "UiStateAnimation"
            ) { state ->
                when (state) {
                    is FaqUiState.Idle, is FaqUiState.Error -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (state is FaqUiState.Error) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            TopicInputSection(
                                topicInput = topicInput,
                                onTopicInputChanged = viewModel::onTopicInputChanged,
                                selectedDepth = selectedDepth,
                                onDepthSelected = viewModel::onDepthSelected,
                                onGenerate = viewModel::generateFaq,
                                onSampleSelected = viewModel::selectSampleTopic,
                                onClear = viewModel::clearInput
                            )
                        }
                    }

                    is FaqUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("loading_view"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(54.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Formatting headings, Q&As, tips & recommendations...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is FaqUiState.Success -> {
                        FaqDisplayView(
                            faqResponse = state.faqResponse,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            selectedFilter = selectedFilter,
                            onFilterSelected = viewModel::onSectionFilterChanged,
                            speakingText = speakingText,
                            onSpeakText = viewModel::speakText,
                            onBackToInput = viewModel::resetToInput
                        )
                    }
                }
            }

            if (isHistoryOpen) {
                FaqHistorySheet(
                    sheetState = sheetState,
                    savedFaqs = savedFaqs,
                    onSelectFaq = { savedFaq ->
                        viewModel.loadSavedFaq(savedFaq)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { isHistoryOpen = false }
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDeleteFaq = viewModel::deleteSavedFaq,
                    onDismiss = { isHistoryOpen = false }
                )
            }
        }
    }
}
