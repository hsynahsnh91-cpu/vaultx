package com.example.ai

import com.example.BuildConfig
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
import java.util.concurrent.TimeUnit

data class AiFolderSuggestion(
    val itemId: String,
    val itemTitle: String,
    val currentFolder: String,
    val suggestedFolder: String,
    val confidence: String, // "High", "Medium", "Low"
    val reason: String,
    var isApproved: Boolean = true
)

data class AiDocumentAnalysisResult(
    val operation: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLocalProcessing: Boolean = false
)

data class AiSearchResult(
    val item: VaultItem,
    val reason: String,
    val score: Int
)

object VaultAiManager {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    /**
     * Checks if Gemini API Key is available in BuildConfig.
     */
    fun isGeminiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Calls Gemini 3.5 Flash REST endpoint with timeout protection.
     */
    suspend fun callGeminiApi(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured in Secrets panel."))
        }

        val url = "$BASE_URL$GEMINI_MODEL:generateContent?key=$apiKey"
        val jsonPayload = """
            {
              "contents": [{
                "parts": [{"text": ${moshi.adapter(String::class.java).toJson(prompt)}}]
              }],
              "generationConfig": {
                "temperature": 0.2
              }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Gemini API error (${response.code}): $errBody"))
                }
                val body = response.body?.string() ?: ""
                val text = parseGeminiResponseText(body)
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeminiResponseText(json: String): String {
        return try {
            val jsonMap = moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *>
            val candidates = jsonMap?.get("candidates") as? List<*>
            val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
            val content = firstCandidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            (firstPart?.get("text") as? String)?.trim() ?: json
        } catch (_: Exception) {
            json
        }
    }

    /**
     * Local heuristic classification fallback for offline use, privacy preservation, or when API key is unset.
     */
    fun classifyItemLocally(item: VaultItem): AiFolderSuggestion {
        val name = (item.fileName ?: item.title).lowercase()
        val tags = item.tags.map { it.lowercase() }
        val ext = name.substringAfterLast(".", "")

        val (folder, confidence, reason) = when {
            name.contains("tax") || name.contains("invoice") || name.contains("bank") || name.contains("salary") ||
                    name.contains("receipt") || tags.any { it.contains("finance") } -> {
                Triple("Finance", "High", "Identified financial and transactional terminology in filename and tags.")
            }
            name.contains("study") || name.contains("exam") || name.contains("lecture") || name.contains("course") ||
                    name.contains("homework") || ext in listOf("epub", "mobi") -> {
                Triple("Study", "High", "Contains academic and educational course materials.")
            }
            name.contains("contract") || name.contains("agreement") || name.contains("nda") || name.contains("legal") ||
                    name.contains("policy") || name.contains("terms") -> {
                Triple("Legal", "High", "Identified formal legal agreements and compliance documents.")
            }
            name.contains("project") || name.contains("work") || name.contains("sprint") || name.contains("meeting") ||
                    name.contains("report") || name.contains("spec") -> {
                Triple("Work", "Medium", "Contains workplace and professional collaboration metadata.")
            }
            name.contains("passport") || name.contains("id") || name.contains("license") || name.contains("medical") ||
                    name.contains("insurance") || item.type == VaultType.RECORD -> {
                Triple("Personal", "High", "Sensitive identity, healthcare, or personal records.")
            }
            ext in listOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mkv", "mp3", "wav") -> {
                Triple("Media", "High", "Rich media formats and audiovisual assets.")
            }
            ext in listOf("pdf", "doc", "docx", "txt", "md", "csv", "xlsx") -> {
                Triple("Documents", "Medium", "Standard office document formats.")
            }
            else -> {
                Triple("Archive", "Low", "General unclassified vault items recommended for archiving.")
            }
        }

        return AiFolderSuggestion(
            itemId = item.id,
            itemTitle = item.title,
            currentFolder = item.folder,
            suggestedFolder = folder,
            confidence = confidence,
            reason = reason,
            isApproved = item.folder != folder
        )
    }

    /**
     * Generates intelligent folder organization suggestions for vault items.
     * Uses cloud Gemini if user consented and key is available, otherwise uses local heuristic classification.
     */
    suspend fun generateOrganizationSuggestions(
        items: List<VaultItem>,
        useCloudAi: Boolean = false
    ): List<AiFolderSuggestion> = withContext(Dispatchers.Default) {
        if (!useCloudAi || !isGeminiKeyAvailable()) {
            return@withContext items.map { classifyItemLocally(it) }
        }

        // Cloud batch suggestion using Gemini
        val fileListing = items.take(40).joinToString("\n") {
            "- ID: ${it.id} | Title: ${it.title} | CurrentFolder: ${it.folder} | Type: ${it.type} | Tags: ${it.tags.joinToString(",")}"
        }

        val prompt = """
            You are a secure personal file organizer.
            Categorize each of the following vault items into one of these standard folders:
            [Work, Study, Finance, Personal, Legal, Documents, Media, Archive].
            For each item, output one line formatted EXACTLY as:
            ID|SuggestedFolder|Confidence(High/Medium/Low)|ShortReason
            
            Items:
            $fileListing
        """.trimIndent()

        val response = callGeminiApi(prompt).getOrNull()
        if (response.isNullOrBlank()) {
            return@withContext items.map { classifyItemLocally(it) }
        }

        val parsedMap = mutableMapOf<String, Triple<String, String, String>>()
        response.lines().forEach { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 4) {
                val id = parts[0].removePrefix("- ").removePrefix("ID:").trim()
                parsedMap[id] = Triple(parts[1], parts[2], parts[3])
            }
        }

        items.map { item ->
            val aiProposal = parsedMap[item.id]
            if (aiProposal != null) {
                AiFolderSuggestion(
                    itemId = item.id,
                    itemTitle = item.title,
                    currentFolder = item.folder,
                    suggestedFolder = aiProposal.first,
                    confidence = aiProposal.second,
                    reason = aiProposal.third,
                    isApproved = item.folder != aiProposal.first
                )
            } else {
                classifyItemLocally(item)
            }
        }
    }

    /**
     * Performs AI Document Understanding operations (Summarize, Dates, Tasks, Topics, Outline, Q&A).
     */
    suspend fun analyzeDocument(
        textSample: String,
        operation: String,
        question: String? = null,
        useCloudAi: Boolean = false
    ): AiDocumentAnalysisResult = withContext(Dispatchers.Default) {
        val sanitizedText = textSample.take(15000)

        if (useCloudAi && isGeminiKeyAvailable()) {
            val prompt = when (operation) {
                "SUMMARIZE" -> "Summarize the key points of this document concisely in bullet points:\n\n$sanitizedText"
                "DATES" -> "Extract all important dates, deadlines, and milestones mentioned in this document with their context:\n\n$sanitizedText"
                "TASKS" -> "Extract all actionable tasks, to-dos, and responsibilities mentioned in this document:\n\n$sanitizedText"
                "TOPICS" -> "Extract the main key topics and thematic categories covered in this document:\n\n$sanitizedText"
                "OUTLINE" -> "Generate a clean hierarchical outline of the document structure:\n\n$sanitizedText"
                "QUESTION" -> "Answer this question based exclusively on the provided document:\nQuestion: ${question ?: ""}\n\nDocument:\n$sanitizedText"
                else -> "Summarize the document:\n\n$sanitizedText"
            }

            val cloudResult = callGeminiApi(prompt)
            if (cloudResult.isSuccess) {
                return@withContext AiDocumentAnalysisResult(
                    operation = operation,
                    content = cloudResult.getOrThrow(),
                    isLocalProcessing = false
                )
            }
        }

        // On-device local semantic processing fallback
        val localOutput = when (operation) {
            "SUMMARIZE" -> {
                val lines = sanitizedText.lines().filter { it.isNotBlank() }
                "Document Overview:\n• Total length: ${sanitizedText.length} characters (${lines.size} paragraphs)\n" +
                        "• Sample key excerpts:\n" + lines.take(4).joinToString("\n") { "  - ${it.take(120)}..." }
            }
            "DATES" -> {
                val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \d{1,2},? \d{4}|\b20\d{2}\b)""", RegexOption.IGNORE_CASE)
                val matches = dateRegex.findAll(sanitizedText).map { it.value }.distinct().take(10).toList()
                if (matches.isNotEmpty()) {
                    "Identified timeline references in document:\n" + matches.joinToString("\n") { "• $it" }
                } else {
                    "No explicit calendar dates were detected in the text sample."
                }
            }
            "TASKS" -> {
                val taskRegex = Regex("""(?i)\b(todo|action|must|should|review|submit|due|verify|update|complete)\b[^\n.!?]{5,100}""")
                val tasks = taskRegex.findAll(sanitizedText).map { it.value.trim() }.distinct().take(8).toList()
                if (tasks.isNotEmpty()) {
                    "Identified action items:\n" + tasks.joinToString("\n") { "• $it" }
                } else {
                    "No explicit action verbs or to-do directives were found in the sample text."
                }
            }
            "TOPICS" -> {
                val words = sanitizedText.split(Regex("""[\s,.;:!?()]+"""))
                    .filter { it.length > 4 && !it.matches(Regex("""(?i)\b(about|after|before|could|their|there|would|which|where)\b""")) }
                    .groupBy { it.lowercase() }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(8)
                "Key recurring thematic keywords:\n" + words.joinToString("\n") { "• ${it.first} (${it.second} occurrences)" }
            }
            "OUTLINE" -> {
                val headers = sanitizedText.lines().filter { it.trim().startsWith("#") || it.length < 50 && it.endsWith(":") }
                if (headers.isNotEmpty()) {
                    "Document Structure Outline:\n" + headers.take(12).joinToString("\n") { "• $it" }
                } else {
                    "Sectional Outline:\n• Section 1: Introduction\n• Section 2: Core Body Details\n• Section 3: Summary & Notes"
                }
            }
            "QUESTION" -> {
                val q = question?.lowercase() ?: ""
                val sentences = sanitizedText.split(Regex("""(?<=[.!?])\s+"""))
                val relevant = sentences.filter { s -> q.split(" ").filter { it.length > 3 }.any { s.lowercase().contains(it) } }
                if (relevant.isNotEmpty()) {
                    "Relevant excerpts matching your query:\n\n" + relevant.take(3).joinToString("\n\n") { "• $it" }
                } else {
                    "No direct textual matches found for '${question ?: ""}' in the extracted document text."
                }
            }
            else -> "Local document analysis completed (${sanitizedText.length} characters processed)."
        }

        AiDocumentAnalysisResult(
            operation = operation,
            content = localOutput,
            isLocalProcessing = true
        )
    }

    /**
     * Natural-language semantic search across vault items.
     * Evaluates title, folder, tags, and MIME types with semantic intent.
     */
    fun naturalLanguageSearch(
        query: String,
        items: List<VaultItem>
    ): List<AiSearchResult> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) return emptyList()

        val tokens = trimmed.split(" ").filter { it.length > 2 }

        val results = mutableListOf<AiSearchResult>()

        for (item in items) {
            val titleLower = item.title.lowercase()
            val folderLower = item.folder.lowercase()
            val tagsLower = item.tags.map { it.lowercase() }
            val fileNameLower = (item.fileName ?: "").lowercase()

            var score = 0
            val reasons = mutableListOf<String>()

            // Direct query match
            if (titleLower.contains(trimmed) || fileNameLower.contains(trimmed)) {
                score += 50
                reasons.add("Exact match in item title")
            }

            // Keyword intent matching
            for (token in tokens) {
                if (titleLower.contains(token)) {
                    score += 20
                    reasons.add("Title contains '$token'")
                }
                if (folderLower.contains(token)) {
                    score += 15
                    reasons.add("In folder '$token'")
                }
                if (tagsLower.any { it.contains(token) }) {
                    score += 15
                    reasons.add("Tagged with '$token'")
                }
            }

            // Semantic category inference
            if ((trimmed.contains("photo") || trimmed.contains("picture") || trimmed.contains("image")) &&
                (item.fileMimeType?.startsWith("image/") == true)) {
                score += 25
                reasons.add("Matched image file type")
            }
            if ((trimmed.contains("video") || trimmed.contains("movie") || trimmed.contains("recording")) &&
                (item.fileMimeType?.startsWith("video/") == true)) {
                score += 25
                reasons.add("Matched video file type")
            }
            if ((trimmed.contains("doc") || trimmed.contains("pdf") || trimmed.contains("contract") || trimmed.contains("report")) &&
                (item.fileMimeType?.contains("pdf") == true || item.fileMimeType?.contains("document") == true)) {
                score += 25
                reasons.add("Matched document format")
            }
            if ((trimmed.contains("credential") || trimmed.contains("password") || trimmed.contains("login")) &&
                item.type == VaultType.PASSWORD) {
                score += 30
                reasons.add("Matched Password vault type")
            }
            if ((trimmed.contains("protected") || trimmed.contains("locked") || trimmed.contains("secret")) &&
                item.isPasswordProtected) {
                score += 20
                reasons.add("Item is password-protected")
            }

            if (score > 0) {
                results.add(
                    AiSearchResult(
                        item = item,
                        reason = reasons.distinct().joinToString(" • "),
                        score = score
                    )
                )
            }
        }

        return results.sortedByDescending { it.score }
    }
}
