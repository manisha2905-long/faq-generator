package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FaqItem
import com.example.data.model.FaqResponse
import com.example.data.model.FaqSection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FaqDisplayView(
    faqResponse: FaqResponse,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    speakingText: String?,
    onSpeakText: (String) -> Unit,
    onBackToInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Keep track of expanded state for Q&As
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    // Section list filters
    val allSectionNames = listOf("All") + faqResponse.sections.map { it.sectionName } + listOf("Tips", "Recommendations")

    // Filtered Q&As
    val filteredSections = faqResponse.sections.mapNotNull { section ->
        if (selectedFilter != "All" && selectedFilter != section.sectionName && selectedFilter != "Tips" && selectedFilter != "Recommendations") {
            null
        } else {
            val matchingItems = section.items.filter { item ->
                searchQuery.isBlank() ||
                        item.question.contains(searchQuery, ignoreCase = true) ||
                        item.answer.contains(searchQuery, ignoreCase = true)
            }
            if (matchingItems.isNotEmpty()) FaqSection(section.sectionName, matchingItems) else null
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP BANNER: Title & Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "FAQ: ${faqResponse.topic}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (faqResponse.summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = faqResponse.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ACTION BUTTONS BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                copyFullFaqToClipboard(context, faqResponse)
                            },
                            label = { Text("Copy Markdown") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("copy_faq_button")
                        )

                        AssistChip(
                            onClick = {
                                shareFaqText(context, faqResponse)
                            },
                            label = { Text("Share") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("share_faq_button")
                        )
                    }
                }
            }
        }

        // SEARCH BAR inside FAQ
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_faq_input"),
                placeholder = { Text("Search questions or keywords...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // FILTER CHIPS
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allSectionNames.forEach { sectionName ->
                    val isSelected = sectionName == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(sectionName) },
                        label = { Text(sectionName) }
                    )
                }
            }
        }

        // FAQ SECTIONS & Q&A CARDS
        if (selectedFilter != "Tips" && selectedFilter != "Recommendations") {
            items(filteredSections, key = { it.sectionName }) { section ->
                FaqSectionBlock(
                    section = section,
                    expandedStates = expandedStates,
                    speakingText = speakingText,
                    onSpeakText = onSpeakText,
                    onCopyItem = { item ->
                        copyTextToClipboard(context, "Q: ${item.question}\nA: ${item.answer}")
                    }
                )
            }
        }

        // TIPS SECTION
        if ((selectedFilter == "All" || selectedFilter == "Tips") && faqResponse.tips.isNotEmpty()) {
            item {
                TipsCardBlock(
                    tips = faqResponse.tips,
                    onCopyTip = { tip -> copyTextToClipboard(context, tip) }
                )
            }
        }

        // ACTIONABLE RECOMMENDATIONS SECTION
        if ((selectedFilter == "All" || selectedFilter == "Recommendations") && faqResponse.recommendations.isNotEmpty()) {
            item {
                RecommendationsCardBlock(
                    recommendations = faqResponse.recommendations,
                    onCopyRec = { rec -> copyTextToClipboard(context, rec) }
                )
            }
        }

        // BACK TO TOP / NEW FAQ BUTTON
        item {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBackToInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_another_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Generate Another FAQ",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqSectionBlock(
    section: FaqSection,
    expandedStates: MutableMap<String, Boolean>,
    speakingText: String?,
    onSpeakText: (String) -> Unit,
    onCopyItem: (FaqItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "## ${section.sectionName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        section.items.forEachIndexed { index, item ->
            val key = "${section.sectionName}_$index"
            val isExpanded = expandedStates[key] ?: true // Default expanded for easy reading

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStates[key] = !isExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q${index + 1}. ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = item.question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { expandedStates[key] = !isExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = item.answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onSpeakText("${item.question}. ${item.answer}") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Listen",
                                        tint = if (speakingText == "${item.question}. ${item.answer}") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onCopyItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Q&A",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipsCardBlock(
    tips: List<String>,
    onCopyTip: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "## Tips & Best Practices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            tips.forEach { tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationsCardBlock(
    recommendations: List<String>,
    onCopyRec: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "## Actionable Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            recommendations.forEachIndexed { idx, rec ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${idx + 1}. ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = rec,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun copyFullFaqToClipboard(context: Context, faq: FaqResponse) {
    val builder = StringBuilder()
    builder.appendLine("# FAQ: ${faq.topic}\n")
    if (faq.summary.isNotBlank()) {
        builder.appendLine("${faq.summary}\n")
    }

    faq.sections.forEach { section ->
        builder.appendLine("## ${section.sectionName}")
        section.items.forEachIndexed { i, item ->
            builder.appendLine("Q${i + 1}: ${item.question}")
            builder.appendLine("A: ${item.answer}\n")
        }
    }

    if (faq.tips.isNotEmpty()) {
        builder.appendLine("## Tips")
        faq.tips.forEach { tip -> builder.appendLine("- $tip") }
        builder.appendLine()
    }

    if (faq.recommendations.isNotEmpty()) {
        builder.appendLine("## Actionable Recommendations")
        faq.recommendations.forEachIndexed { i, rec -> builder.appendLine("${i + 1}. $rec") }
    }

    copyTextToClipboard(context, builder.toString())
}

private fun shareFaqText(context: Context, faq: FaqResponse) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "FAQ: ${faq.topic}")
        putExtra(Intent.EXTRA_TEXT, "FAQ: ${faq.topic}\n\n${faq.summary}\n\nGenerated with FAQ Generator app.")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share FAQ"))
}

private fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("FAQ Content", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}
