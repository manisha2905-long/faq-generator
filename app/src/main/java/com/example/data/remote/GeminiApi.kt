package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.FaqDepth
import com.example.data.model.FaqItem
import com.example.data.model.FaqResponse
import com.example.data.model.FaqSection
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String? = null)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiResponseFormatText(val mimeType: String)

@JsonClass(generateAdapter = true)
data class GeminiResponseFormat(val text: GeminiResponseFormatText? = null)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseFormat: GeminiResponseFormat? = null,
    val temperature: Float? = 0.7f
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent?)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiNetworkClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class FaqGeneratorService {

    suspend fun generateFaq(topicInput: String, depth: FaqDepth): FaqResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                return callGeminiApi(topicInput, depth, apiKey)
            } catch (e: Exception) {
                // If remote call fails, fallback gracefully
                e.printStackTrace()
            }
        }

        return generateFallbackFaq(topicInput, depth)
    }

    private suspend fun callGeminiApi(topicInput: String, depth: FaqDepth, apiKey: String): FaqResponse {
        val systemPrompt = """
            You are an expert FAQ assistant.
            Task: Generate a clear, well-organized FAQ based on the user's topic or input text.
            Requirements:
            - ${depth.promptInstruction}
            - Structure output into logical sections (General Questions, Step-by-Step, Pitfalls & Common Mistakes).
            - Include 3-5 concise, practical Tips.
            - Include 3-5 Actionable Recommendations for beginners.
            - Return ONLY JSON matching this structure exactly without any extra text or markdown formatting:
            {
              "topic": "Clean Display Title",
              "summary": "Short 2-3 sentence overview.",
              "sections": [
                {
                  "sectionName": "General Questions",
                  "items": [
                    {"question": "What is ...?", "answer": "Clear explanation..."}
                  ]
                }
              ],
              "tips": ["Tip 1", "Tip 2"],
              "recommendations": ["Recommendation 1", "Recommendation 2"]
            }
        """.trimIndent()

        val userPrompt = "Topic or Content:\n$topicInput"

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                responseFormat = GeminiResponseFormat(
                    text = GeminiResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.7f
            )
        )

        val response = GeminiNetworkClient.service.generateContent(apiKey, request)
        val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from Gemini API")

        val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()
        val adapter = GeminiNetworkClient.moshi.adapter(FaqResponse::class.java)
        return adapter.fromJson(cleanJson) ?: throw IllegalStateException("Failed to parse FAQ JSON response")
    }

    fun generateFallbackFaq(topicInput: String, depth: FaqDepth): FaqResponse {
        val cleanTopic = topicInput.trim().take(60).ifEmpty { "General Topic" }
        val topicLower = cleanTopic.lowercase()

        val isFlutter = topicLower.contains("flutter")
        val isKotlin = topicLower.contains("kotlin") || topicLower.contains("android")
        val isAi = topicLower.contains("ai") || topicLower.contains("machine learning") || topicLower.contains("gemini")

        val summaryText = when {
            isFlutter -> "Flutter is Google's open-source UI toolkit for building natively compiled applications for mobile, web, desktop, and embedded devices from a single codebase using Dart."
            isKotlin -> "Kotlin is a modern, cross-platform, statically typed programming language designed to interoperate fully with Java, now the official standard for Android app development."
            isAi -> "Artificial Intelligence and Machine Learning enable software systems to learn from data, recognize patterns, and automate decision-making across diverse domains."
            else -> "Comprehensive beginner-friendly overview for $cleanTopic, covering core principles, step-by-step setup, common pitfalls, and practical recommendations."
        }

        val generalQuestions = when {
            isFlutter -> listOf(
                FaqItem("What is Flutter?", "Flutter is a UI framework created by Google that allows developers to create mobile, web, and desktop apps from a single Dart codebase."),
                FaqItem("Why use Flutter over native development?", "Flutter offers hot reload for instant preview, consistent cross-platform UI components, high performance rendered via Skia/Impeller, and faster time-to-market."),
                FaqItem("What language does Flutter use?", "Flutter uses Dart, an easy-to-learn object-oriented language developed by Google with strong typing and asynchronous primitives.")
            )
            isKotlin -> listOf(
                FaqItem("What is Kotlin?", "Kotlin is a modern programming language developed by JetBrains that is modern, concise, safe, and fully interoperable with Java."),
                FaqItem("Why choose Kotlin for Android development?", "Kotlin reduces boilerplate code by up to 40%, prevents null pointer exceptions with built-in null safety, and supports modern coroutines for asynchronous work."),
                FaqItem("Can I use Kotlin for multiplatform apps?", "Yes, Kotlin Multiplatform (KMP) allows sharing core business logic across Android, iOS, Desktop, and Web while preserving native UIs.")
            )
            else -> listOf(
                FaqItem("What is $cleanTopic?", "$cleanTopic encompasses the core concepts, practical techniques, and best practices involved in learning and applying this subject effectively."),
                FaqItem("Why is $cleanTopic important?", "Understanding $cleanTopic helps build foundational skills, improves efficiency, solves real-world challenges, and opens new opportunities."),
                FaqItem("Who is this FAQ guide designed for?", "This guide is structured specifically for beginners seeking clear, step-by-step answers and actionable recommendations.")
            )
        }

        val stepByStepQuestions = listOf(
            FaqItem("How do I get started with $cleanTopic?", "1. Define your initial goal or project scope.\n2. Set up the necessary tools or workspace.\n3. Follow standard beginner tutorials and build a basic sample project.\n4. Practice consistently and expand gradually."),
            FaqItem("What essential tools or prerequisites are needed?", "Ensure you have a modern computer or working environment, access to documentation, reliable tools, and a structured learning routine.")
        )

        val pitfallQuestions = listOf(
            FaqItem("What are common mistakes to avoid?", "Avoid skipping foundational concepts, rushing into complex advanced topics too early, neglecting practical exercises, and working without clear goals."),
            FaqItem("How can I troubleshoot when stuck?", "Break down the issue into smaller testable parts, check official documentation, search community forums, and test step-by-step.")
        )

        val tips = listOf(
            "Start small: Build a minimal working project before adding advanced features.",
            "Document your learning: Write down key steps and code/notes for quick reference.",
            "Join community forums and discussions to learn from experienced practitioners.",
            "Focus on understanding core principles rather than memorizing syntax or procedures."
        )

        val recommendations = listOf(
            "Set aside 20-30 minutes of dedicated practice time each day.",
            "Complete one hands-on project from scratch to build confidence.",
            "Review official documentation and verified beginner guides.",
            "Share your work with peers for constructive feedback."
        )

        return FaqResponse(
            topic = cleanTopic,
            summary = summaryText,
            sections = listOf(
                FaqSection("General Questions", generalQuestions),
                FaqSection("Step-by-Step & Getting Started", stepByStepQuestions),
                FaqSection("Common Pitfalls & Mistakes", pitfallQuestions)
            ),
            tips = tips,
            recommendations = recommendations
        )
    }
}
