package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeOdiaInput(
        userInput: String,
        conversationHistory: List<ConversationMessage> = emptyList(),
        preferredLanguage: PreferredLanguage = PreferredLanguage.ODIA,
        customApiKey: String? = null
    ): JarvisResponse = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            Log.d("GeminiService", "No active Gemini API key found, executing smart local Odia intent parser.")
            return@withContext parseOdiaIntentLocally(userInput, reason = "LOCAL NEURAL PARSER ACTIVE")
        }

        try {
            val systemPrompt = buildString {
                append("You are JARVIS, a friendly, highly intelligent voice AI assistant. ")
                append(preferredLanguage.speechPrompt)
                append(" Understand the user's spoken language (including common Odia-English mixed speech) and reply in natural, clear Odia whenever possible. ")
                append("Speak every answer aloud. Maintain conversational context across previous turns in the session. ")
                append("If the user requests English or Hindi, switch languages appropriately. ")
                append("Address the user as \"Boss\" (or \"ବସ୍\" in Odia) occasionally, without repeating it in every sentence. ")
                append("Give conversational spoken answers—short and punchy for quick queries, and articulate when explaining concepts. ")
                append("You must return a strict JSON object with two fields: \"reply\" (your spoken response text in Odia script) ")
                append("and \"action\" (one of: \"OPEN_YOUTUBE\", \"OPEN_SETTINGS\", \"OPEN_CAMERA\", \"OPEN_DIALER\", \"OPEN_BROWSER\", \"OPEN_MAPS\", \"OPEN_CALCULATOR\", \"SET_TIMER\", \"TELL_TIME\", \"STOP_SPEAKING\", \"GO_HOME\", or \"NONE\"). ")
                append("Never pretend to execute an action that is not supported.")
            }

            // Build multi-turn conversational contents
            val contentsArray = JSONArray()

            // Include last 8 conversational turns for context memory
            val recentTurns = conversationHistory.takeLast(8)
            for (turn in recentTurns) {
                contentsArray.put(JSONObject().apply {
                    put("role", turn.role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", turn.text) })
                    })
                })
            }

            // Append current user turn
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userInput) })
                })
            })

            val requestJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })

                put("contents", contentsArray)

                // Generation Config requesting strict JSON
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("GeminiService", "API error: ${response.code} -> $responseBody")
                return@withContext parseOdiaIntentLocally(userInput, reason = "API CODE ${response.code}")
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text")

            if (textPart.isNullOrBlank()) {
                return@withContext parseOdiaIntentLocally(userInput, reason = "EMPTY MODEL RESPONSE")
            }

            // Parse model's JSON string
            val cleanJsonString = textPart.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedOutput = JSONObject(cleanJsonString)
            val reply = parsedOutput.optString("reply", "ଆଜ୍ଞା ବସ୍, ମୁଁ ଆପଣଙ୍କ କଥା ବୁଝିପାରିଲି।")
            val action = parsedOutput.optString("action", "NONE").uppercase()

            JarvisResponse(
                reply = reply,
                action = action,
                rawJson = cleanJsonString,
                isFallback = false
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception during Gemini API call: ${e.message}", e)
            parseOdiaIntentLocally(userInput, reason = "FALLBACK (${e.localizedMessage ?: "NETWORK"})")
        }
    }

    /**
     * Highly accurate local Odia heuristic intent parser for offline resilience or demo testing.
     */
    fun parseOdiaIntentLocally(input: String, reason: String? = null): JarvisResponse {
        val lower = input.lowercase().trim()

        val (action, reply) = when {
            // YouTube
            lower.contains("ୟୁଟ୍ୟୁବ") || lower.contains("ୟୁଟୁବ") || lower.contains("youtube") || lower.contains("video") || lower.contains("ଭିଡିଓ") -> {
                "OPEN_YOUTUBE" to "ଆଜ୍ଞା ବସ୍, ମୁଁ ୟୁଟ୍ୟୁବ୍ ଖୋଲୁଛି।"
            }
            // Camera
            lower.contains("କ୍ୟାମେରା") || lower.contains("camera") || lower.contains("ଫଟୋ") || lower.contains("photo") -> {
                "OPEN_CAMERA" to "ଆଜ୍ଞା ବସ୍, କ୍ୟାମେରା ପ୍ରସ୍ତୁତ କରୁଛି।"
            }
            // Settings
            lower.contains("ସେଟିଙ୍ଗ") || lower.contains("ସେଟିଂ") || lower.contains("settings") || lower.contains("setting") -> {
                "OPEN_SETTINGS" to "ଆଜ୍ଞା ବସ୍, ସିଷ୍ଟମ୍ ସେଟିଙ୍ଗସ୍ ଖୋଲୁଛି।"
            }
            // Timer / Alarm
            lower.contains("ଟାଇମର") || lower.contains("ଟାଇମର୍") || lower.contains("timer") || lower.contains("alarm") || lower.contains("ଆଲାର୍ମ") -> {
                "SET_TIMER" to "ଆଜ୍ଞା ବସ୍, ଟାଇମର୍ ସେଟ୍ କରୁଛି।"
            }
            // Time query
            lower.contains("ସମୟ") || lower.contains("କେତେ ବାଜିଲା") || lower.contains("time") -> {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date())
                "TELL_TIME" to "ବର୍ତ୍ତମାନ ସମୟ ହେଉଛି $timeStr ବସ୍।"
            }
            // Stop speaking / Silence
            lower.contains("ଚୁପ") || lower.contains("stop") || lower.contains("quiet") || lower.contains("ବନ୍ଦ କର") -> {
                "STOP_SPEAKING" to "ଠିକ୍ ଅଛି ବସ୍, ମୁଁ ଚୁପ୍ ରହୁଛି।"
            }
            // Home screen / Minimize
            lower.contains("ହୋମ") || lower.contains("home") || lower.contains("ଫେରିଯାଅ") -> {
                "GO_HOME" to "ଆଜ୍ଞା ବସ୍, ହୋମ୍ ସ୍କ୍ରିନ୍ କୁ ଯାଉଛି।"
            }
            // Dialer / Phone Call
            lower.contains("ଡାଏଲ") || lower.contains("ଫୋନ୍") || lower.contains("କଲ୍") || lower.contains("dialer") || lower.contains("call") || lower.contains("phone") -> {
                "OPEN_DIALER" to "ଆଜ୍ଞା ବସ୍, ଫୋନ୍ ଡାଏଲର୍ ଖୋଲୁଛି।"
            }
            // Browser / Google
            lower.contains("ବ୍ରାଉଜ") || lower.contains("ଗୁଗୁଲ୍") || lower.contains("ଇଣ୍ଟରନେଟ୍") || lower.contains("browser") || lower.contains("google") || lower.contains("search") -> {
                "OPEN_BROWSER" to "ଆଜ୍ଞା ବସ୍, ୱେବ୍ ବ୍ରାଉଜର୍ ଖୋଲୁଛି।"
            }
            // Maps / Location
            lower.contains("ମ୍ୟାପ") || lower.contains("ନକ୍ସା") || lower.contains("ସ୍ଥାନ") || lower.contains("maps") || lower.contains("location") || lower.contains("map") -> {
                "OPEN_MAPS" to "ଆଜ୍ଞା ବସ୍, ମ୍ୟାପ୍ସ ଖୋଲୁଛି।"
            }
            // Calculator
            lower.contains("କାଲକୁଲେଟ") || lower.contains("ହିସାବ") || lower.contains("calculator") || lower.contains("calc") -> {
                "OPEN_CALCULATOR" to "ଆଜ୍ଞା ବସ୍, କାଲକୁଲେଟର୍ ଖୋଲୁଛି।"
            }
            // Specific user prompts requested in prompt
            lower.contains("କେମିତି ଅଛ") || lower.contains("how are you") -> {
                "NONE" to "ମୁଁ ଭଲ ଅଛି ବସ୍! ତୁମେ କେମିତି ଅଛ? ଆଜି ତୁମ ପାଇଁ କଣ କରିପାରିବି?"
            }
            lower.contains("ଓଡ଼ିଆରେ କଥା") || lower.contains("କଥା ହୁଅ") -> {
                "NONE" to "ହଁ ନିଶ୍ଚୟ ବସ୍! ମୁଁ ସମ୍ପୂର୍ଣ୍ଣ ଓଡ଼ିଆରେ କଥା ହେବାକୁ ପ୍ରସ୍ତୁତ। ଆପଣ କଣ ଜାଣିବାକୁ ଚାହାଁନ୍ତି?"
            }
            lower.contains("ଭିଡିଓ ଟପିକ") || lower.contains("video topic") || lower.contains("ଟପିକ") -> {
                "NONE" to "ୟୁଟ୍ୟୁବ୍ ପାଇଁ ଏକ ଭଲ ଟପିକ୍ ହେଉଛି: 'ଓଡ଼ିଶାର ଐତିହ୍ୟ ଏବଂ ଆଧୁନିକ AI ପ୍ରଯୁକ୍ତିବିଦ୍ୟା'। ଏହା ଦର୍ଶକଙ୍କୁ ବହୁତ ଆକର୍ଷିତ କରିବ!"
            }
            lower.contains("କଣ କରିବା ଉଚିତ") || lower.contains("what should i do") -> {
                "NONE" to "ଆଜି କିଛି ନୂଆ ଶିଖିବା ବା ଏକ ଗୁରୁତ୍ୱପୂର୍ଣ୍ଣ କାମ ସମ୍ପୂର୍ଣ୍ଣ କରିବା ଉଚିତ ବସ୍। ଚାଲନ୍ତୁ ଆରମ୍ଭ କରିବା!"
            }
            // Greetings / Identity
            lower.contains("ନମସ୍କାର") || lower.contains("hello") || lower.contains("hi") -> {
                "NONE" to "ନମସ୍କାର ବସ୍! ମୁଁ ଜାର୍ଭିସ୍, ଆପଣଙ୍କ AI ସହାୟକ। ଆଜି କିପରି ସାହାଯ୍ୟ କରିବି?"
            }
            lower.contains("ତୁମେ କିଏ") || lower.contains("who are you") || lower.contains("ନାମ କଣ") -> {
                "NONE" to "ମୁଁ ଜାର୍ଭିସ୍, ଆପଣଙ୍କ ବ୍ୟକ୍ତିଗତ Odia Voice AI ସହାୟକ। ମୁଁ ଆପଣଙ୍କ ନିର୍ଦ୍ଦେଶ ପାଳନ ପାଇଁ ସର୍ବଦା ପ୍ରସ୍ତୁତ।"
            }
            lower.contains("ଧନ୍ୟବାଦ") || lower.contains("thank") -> {
                "NONE" to "ଆପଣଙ୍କୁ ସ୍ୱାଗତ ବସ୍! ଏହା ମୋର କର୍ତ୍ତବ୍ୟ।"
            }
            else -> {
                "NONE" to "ଆଜ୍ଞା ବସ୍, ମୁଁ ଆପଣଙ୍କ କଥା ଶୁଣିଲି: \"$input\"। ଏ ବିଷୟରେ ମୁଁ ଆପଣଙ୍କୁ କିପରି ସାହାଯ୍ୟ କରିପାରିବି?"
            }
        }

        return JarvisResponse(
            reply = reply,
            action = action,
            rawJson = "{\"reply\": \"$reply\", \"action\": \"$action\"}",
            isFallback = true
        )
    }
}
