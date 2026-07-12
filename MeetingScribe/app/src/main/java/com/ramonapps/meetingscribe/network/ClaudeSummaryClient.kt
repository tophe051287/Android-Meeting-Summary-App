package com.ramonapps.meetingscribe.network

import com.ramonapps.meetingscribe.data.ActionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MeetingSummary(
    val summary: String,
    val keyPoints: List<String>,
    val actionItems: List<ActionItem>,
    val decisions: List<String>
)

private const val SYSTEM_PROMPT = """
You are an assistant that turns raw meeting transcripts into a structured recap.
Read the transcript and respond with ONLY a single valid JSON object (no markdown
fences, no commentary, no extra text before or after) matching exactly this schema:

{
  "summary": "a concise 3-5 sentence overview of what the meeting was about and its outcome",
  "key_points": ["short bullet point", "short bullet point", "..."],
  "action_items": [{"task": "what needs to be done", "owner": "person responsible, or 'Unassigned' if unclear"}],
  "decisions": ["decision that was made", "..."]
}

If a section has nothing to report, return an empty array for it (never omit a key).
Keep bullet points short and specific. Attribute owners by name when the transcript
makes it clear who is responsible; otherwise use "Unassigned".
"""

/**
 * Sends a transcript to Anthropic's Messages API and asks for a structured
 * JSON recap (summary, key points, action items, decisions).
 * Docs: https://docs.claude.com/en/api/messages
 */
class ClaudeSummaryClient(private val apiKey: String, private val model: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun summarize(transcript: String): Result<MeetingSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("model", model)
                put("max_tokens", 2000)
                put("system", SYSTEM_PROMPT.trim())
                put("messages", JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", "Transcript:\n\n$transcript")
                    }
                ))
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val rawText: String = client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Summarization failed (${response.code}): ${raw.take(300)}")
                }
                val root = JSONObject(raw)
                val content = root.getJSONArray("content")
                val builder = StringBuilder()
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.optString("type") == "text") {
                        builder.append(block.optString("text"))
                    }
                }
                builder.toString()
            }

            parseSummaryJson(rawText)
        }
    }

    private fun parseSummaryJson(rawText: String): MeetingSummary {
        // Claude is instructed to return raw JSON, but strip fences defensively
        // in case it wraps the answer in ```json ... ```.
        val cleaned = rawText.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleaned)

        val keyPoints = json.optJSONArray("key_points")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        val actionItems = json.optJSONArray("action_items")?.let { arr ->
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                ActionItem(
                    task = obj.optString("task"),
                    owner = obj.optString("owner").ifBlank { "Unassigned" }
                )
            }
        } ?: emptyList()

        val decisions = json.optJSONArray("decisions")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        return MeetingSummary(
            summary = json.optString("summary"),
            keyPoints = keyPoints,
            actionItems = actionItems,
            decisions = decisions
        )
    }
}
