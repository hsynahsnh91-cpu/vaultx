package com.example.ai

import com.example.BuildConfig
import com.example.model.AiDocumentSummary
import com.example.model.AiOrganizeSuggestion
import com.example.model.NaturalLanguageSearchResult
import com.example.model.VaultItem
import com.example.model.VaultType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object AiService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * AI File Organizer: Analyzes items and generates category suggestions.
     * Uses Gemini if consent is granted and key is available, otherwise uses local intelligence.
     */
    suspend fun organizeFiles(
        items: List<VaultItem>,
        hasExternalConsent: Boolean
    ): List<AiOrganizeSuggestion> = withContext(Dispatchers.Default) {
        val fileItems = items.filter { it.type == VaultType.FILE && !it.isTrash }
        if (fileItems.isEmpty()) return@withContext emptyList()

        if (hasExternalConsent && isGeminiConfigured()) {
            try {
                val cloudResult = organizeWithGemini(fileItems)
                if (cloudResult.isNotEmpty()) return@withContext cloudResult
            } catch (_: Exception) {
                // Fall back securely to local rule engine
            }
        }

        // On-device local semantic classification
        fileItems.map { item ->
            classifyItemLocally(item)
        }
    }

    /**
     * AI Document Understanding: Generates summary, dates, tasks, topics and outline.
     */
    suspend fun analyzeDocument(
        fileName: String,
        documentText: String,
        hasExternalConsent: Boolean
    ): AiDocumentSummary = withContext(Dispatchers.Default) {
        if (hasExternalConsent && isGeminiConfigured() && documentText.length > 30) {
            try {
                val cloudSummary = analyzeWithGemini(fileName, documentText)
                if (cloudSummary != null) return@withContext cloudSummary
            } catch (_: Exception) {
                // Fallback to local extraction
            }
        }

        analyzeDocumentLocally(fileName, documentText)
    }

    /**
     * AI Natural Language Search: Resolves natural language user query against items.
     * Returns matching items with explanation and relevance score.
     */
    suspend fun naturalLanguageSearch(
        query: String,
        items: List<VaultItem>
    ): List<NaturalLanguageSearchResult> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        val normalizedQuery = query.lowercase(Locale.getDefault()).trim()
        val tokens = normalizedQuery.split(Regex("[\\s,;:.!?]+")).filter { it.length > 1 }

        val results = mutableListOf<NaturalLanguageSearchResult>()

        for (item in items) {
            if (item.isTrash) continue

            var score = 0f
            val explanations = mutableListOf<String>()

            val name = (item.fileName ?: item.title).lowercase(Locale.getDefault())
            val folder = item.folder.lowercase(Locale.getDefault())
            val tags = item.tags.map { it.lowercase(Locale.getDefault()) }

            // Semantic category matching
            if (normalizedQuery.contains("warranty") || normalizedQuery.contains("guarantee") || normalizedQuery.contains("ضمان")) {
                if (name.contains("warranty") || tags.any { it.contains("warranty") } || folder.contains("warranty") ||
                    name.contains("receipt") || name.contains("فاتورة") || name.contains("ضمان")) {
                    score += 0.8f
                    explanations.add("Contains warranty, receipt or guarantee metadata")
                }
            }

            if (normalizedQuery.contains("exam") || normalizedQuery.contains("study") || normalizedQuery.contains("university") ||
                normalizedQuery.contains("school") || normalizedQuery.contains("امتحان") || normalizedQuery.contains("دراسة")) {
                if (name.contains("exam") || name.contains("lecture") || name.contains("course") || folder.contains("study") ||
                    name.contains("جامعة") || name.contains("محاضرة") || name.contains("امتحان")) {
                    score += 0.85f
                    explanations.add("Academic and educational terms detected")
                }
            }

            if (normalizedQuery.contains("password") || normalizedQuery.contains("credential") || normalizedQuery.contains("login") ||
                normalizedQuery.contains("كلمة مرور") || normalizedQuery.contains("حساب")) {
                if (item.type == VaultType.PASSWORD) {
                    score += 0.9f
                    explanations.add("Matches Password item type")
                }
            }

            if (normalizedQuery.contains("passport") || normalizedQuery.contains("id") || normalizedQuery.contains("identity") ||
                normalizedQuery.contains("جواز") || normalizedQuery.contains("هوية")) {
                if (item.type == VaultType.RECORD || name.contains("passport") || name.contains("id") || name.contains("هوية")) {
                    score += 0.85f
                    explanations.add("Identity and personal document records")
                }
            }

            // Keyword token matching
            for (token in tokens) {
                if (name.contains(token)) {
                    score += 0.4f
                    explanations.add("Matched keyword '$token' in filename")
                }
                if (tags.any { it.contains(token) }) {
                    score += 0.3f
                    explanations.add("Matched tag '$token'")
                }
                if (folder.contains(token)) {
                    score += 0.2f
                    explanations.add("In folder '$folder'")
                }
            }

            if (score > 0.2f) {
                results.add(
                    NaturalLanguageSearchResult(
                        item = item,
                        matchExplanation = explanations.distinct().joinToString(" • "),
                        relevanceScore = score.coerceAtMost(1.0f)
                    )
                )
            }
        }

        results.sortedByDescending { it.relevanceScore }
    }

    private fun classifyItemLocally(item: VaultItem): AiOrganizeSuggestion {
        val name = (item.fileName ?: item.title).lowercase(Locale.getDefault())
        val ext = name.substringAfterLast('.', "")
        val folder = item.folder

        return when {
            name.contains("exam") || name.contains("lecture") || name.contains("homework") || name.contains("study") ||
                    name.contains("calculus") || name.contains("course") || name.contains("syllabus") || name.contains("امتحان") || name.contains("دراسة") -> {
                AiOrganizeSuggestion(
                    itemId = item.id,
                    fileName = item.fileName ?: item.title,
                    currentFolder = folder,
                    suggestedCategory = "Study",
                    confidence = "High",
                    reason = "Detected academic terminology and educational coursework references"
                )
            }

            name.contains("invoice") || name.contains("tax") || name.contains("statement") || name.contains("bank") ||
                    name.contains("salary") || name.contains("receipt") || name.contains("فاتورة") || name.contains("بنك") || name.contains("ضريبة") -> {
                AiOrganizeSuggestion(
                    itemId = item.id,
                    fileName = item.fileName ?: item.title,
                    currentFolder = folder,
                    suggestedCategory = "Finance",
                    confidence = "High",
                    reason = "Contains financial billing, banking, or tax transaction indicators"
                )
            }

            name.contains("contract") || name.contains("agreement") || name.contains("project") || name.contains("resume") ||
                    name.contains("cv") || name.contains("report") || name.contains("meeting") || name.contains("عمل") || name.contains("مشروع") -> {
                AiOrganizeSuggestion(
                    itemId = item.id,
                    fileName = item.fileName ?: item.title,
                    currentFolder = folder,
                    suggestedCategory = "Work",
                    confidence = "High",
                    reason = "Matches business documentation, contracts, or professional workplace assets"
                )
            }

            name.contains("passport") || name.contains("license") || name.contains("visa") || name.contains("health") ||
                    name.contains("family") || name.contains("photo") || name.contains("هوية") || name.contains("شخصي") -> {
                AiOrganizeSuggestion(
                    itemId = item.id,
                    fileName = item.fileName ?: item.title,
                    currentFolder = folder,
                    suggestedCategory = "Personal",
                    confidence = "Medium",
                    reason = "Associated with personal records, identity cards, or private assets"
                )
            }

            ext in listOf("pdf", "doc", "docx", "txt", "rtf", "odt") -> {
                AiOrganizeSuggestion(
                    itemId = item.id,
                    fileName = item.fileName ?: item.title,
                    currentFolder = folder,
                    suggestedCategory = "Documents",
                    confidence = "Medium",
                    reason = "Standard document file format extension"
                )
            }

            else -> {
                AiOrganizeSuggestion(
                    itemId = item.id,
                    fileName = item.fileName ?: item.title,
                    currentFolder = folder,
                    suggestedCategory = "Personal",
                    confidence = "Low",
                    reason = "General storage item categorized by default profile"
                )
            }
        }
    }

    private fun analyzeDocumentLocally(fileName: String, text: String): AiDocumentSummary {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val preview = lines.take(5).joinToString(" ")
        val summary = if (preview.length > 250) preview.take(250) + "..." else preview.ifEmpty { "Document contains ${lines.size} lines." }

        // Extract dates (e.g. 2026-09-25, 25/09/2026, Sept 25, 2026)
        val datePattern = Pattern.compile("(\\b\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b|\\b\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}\\b|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4})", Pattern.CASE_INSENSITIVE)
        val matcher = datePattern.matcher(text)
        val dates = mutableListOf<String>()
        while (matcher.find() && dates.size < 6) {
            dates.add(matcher.group(1))
        }

        // Extract tasks (TODO, Action, Require, Must, Deadline)
        val taskKeywords = listOf("todo", "task", "must", "action", "deadline", "require", "يرجى", "المطلوب", "مهمة")
        val tasks = lines.filter { line ->
            taskKeywords.any { kw -> line.lowercase(Locale.getDefault()).contains(kw) }
        }.take(5)

        // Key topics
        val topics = mutableListOf<String>()
        if (fileName.contains('.')) topics.add("Format: " + fileName.substringAfterLast('.'))
        topics.add("Words: ${text.split(Regex("\\s+")).size}")
        topics.add("Paragraphs: ${lines.size}")

        val outline = lines.filter { it.startsWith("#") || it.startsWith("Chapter") || it.startsWith("Section") || it.startsWith("الباب") || it.startsWith("الفصل") }
            .take(6)
            .ifEmpty { lines.take(3) }

        return AiDocumentSummary(
            summary = summary,
            importantDates = dates.distinct(),
            tasks = tasks,
            keyTopics = topics,
            outline = outline
        )
    }

    private fun isGeminiConfigured(): Boolean {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    private suspend fun organizeWithGemini(items: List<VaultItem>): List<AiOrganizeSuggestion> = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

        val itemDescriptions = items.map { "${it.id}: ${it.fileName ?: it.title} (current folder: ${it.folder})" }.joinToString("\n")
        val prompt = """
            You are VaultX Intelligent File Classifier.
            Classify each file into one of these categories: Study, Work, Documents, Finance, Personal.
            Provide JSON output array in this schema:
            [
              {
                "itemId": "id",
                "suggestedCategory": "category",
                "confidence": "High|Medium|Low",
                "reason": "short explanation"
              }
            ]
            Files:
            $itemDescriptions
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext emptyList()

        val responseBody = response.body?.string() ?: return@withContext emptyList()
        val respObj = JSONObject(responseBody)
        val text = respObj.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val array = JSONArray(text)
        val suggestions = mutableListOf<AiOrganizeSuggestion>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getString("itemId")
            val matchingItem = items.firstOrNull { it.id == id }
            if (matchingItem != null) {
                suggestions.add(
                    AiOrganizeSuggestion(
                        itemId = id,
                        fileName = matchingItem.fileName ?: matchingItem.title,
                        currentFolder = matchingItem.folder,
                        suggestedCategory = obj.optString("suggestedCategory", "Documents"),
                        confidence = obj.optString("confidence", "High"),
                        reason = obj.optString("reason", "Analyzed by AI Classifier"),
                        isApproved = true
                    )
                )
            }
        }
        suggestions
    }

    private suspend fun analyzeWithGemini(fileName: String, text: String): AiDocumentSummary? = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

        val prompt = """
            Analyze this encrypted document ($fileName).
            Provide JSON output matching this schema:
            {
               "summary": "2-3 sentence executive summary",
               "importantDates": ["date1", "date2"],
               "tasks": ["task1", "task2"],
               "keyTopics": ["topic1", "topic2"],
               "outline": ["section1", "section2"]
            }
            Document Content:
            ${text.take(6000)}
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val responseBody = response.body?.string() ?: return@withContext null
        val respObj = JSONObject(responseBody)
        val candidateText = respObj.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val resultObj = JSONObject(candidateText)
        val datesList = mutableListOf<String>()
        val datesArr = resultObj.optJSONArray("importantDates")
        if (datesArr != null) {
            for (i in 0 until datesArr.length()) datesList.add(datesArr.getString(i))
        }

        val tasksList = mutableListOf<String>()
        val tasksArr = resultObj.optJSONArray("tasks")
        if (tasksArr != null) {
            for (i in 0 until tasksArr.length()) tasksList.add(tasksArr.getString(i))
        }

        val topicsList = mutableListOf<String>()
        val topicsArr = resultObj.optJSONArray("keyTopics")
        if (topicsArr != null) {
            for (i in 0 until topicsArr.length()) topicsList.add(topicsArr.getString(i))
        }

        val outlineList = mutableListOf<String>()
        val outlineArr = resultObj.optJSONArray("outline")
        if (outlineArr != null) {
            for (i in 0 until outlineArr.length()) outlineList.add(outlineArr.getString(i))
        }

        AiDocumentSummary(
            summary = resultObj.optString("summary", "Document analyzed."),
            importantDates = datesList,
            tasks = tasksList,
            keyTopics = topicsList,
            outline = outlineList
        )
    }
}
