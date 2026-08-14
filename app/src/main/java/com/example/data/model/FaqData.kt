package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FaqItem(
    val question: String,
    val answer: String
)

@JsonClass(generateAdapter = true)
data class FaqSection(
    val sectionName: String,
    val items: List<FaqItem>
)

@JsonClass(generateAdapter = true)
data class FaqResponse(
    val topic: String,
    val summary: String,
    val sections: List<FaqSection>,
    val tips: List<String>,
    val recommendations: List<String>
)

enum class FaqDepth(val label: String, val promptInstruction: String) {
    BEGINNER("Beginner Friendly", "Keep answers clear, simple, concise, and beginner-accessible with no jargon."),
    INTERMEDIATE("In-Depth", "Provide comprehensive answers with technical nuances, edge cases, and best practices."),
    STEP_BY_STEP("Step-by-Step Guide", "Focus heavily on clear numbered steps, actionable tutorials, and practical walk-throughs.")
}
