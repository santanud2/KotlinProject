package com.example.widget

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Live desk snapshot pulled from Trade Metrics (same Supabase project as the web app). */
data class DeskSnapshot(
    val todayPnl: Double = 0.0,
    val openCount: Int = 0,
    val watchCount: Int = 0,
    val closedToday: Int = 0,
    val winsToday: Int = 0,
    val lossesToday: Int = 0,
    val currencySymbol: String = "₹",
    val zenMode: Boolean = false,
    val cooldownUntilMs: Long? = null,
    val dailyLossLimit: Double? = null,
    val lastSyncOk: Boolean = false
) {
    val winRateToday: Double
        get() = if (closedToday <= 0) 0.0 else (winsToday.toDouble() / closedToday) * 100.0

    val cooldownActive: Boolean
        get() = cooldownUntilMs != null && cooldownUntilMs > System.currentTimeMillis()
}

object SupabaseSync {
    // TODO: Replace with your actual Supabase URL (e.g. https://xyz.supabase.co)
    private const val SUPABASE_URL = "https://fbprttgoplqwtmziyium.supabase.co"
    // TODO: Replace with your actual Supabase Anon Key
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZicHJ0dGdvcGxxd3Rteml5aXVtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc2NDMyOTQsImV4cCI6MjEwMzIxOTI5NH0.juhzSqgZLxcQ_9v7jhLLauTWO8uC7unZGW1hEwa7USE"

    private var currentUserUid: String? = null
    private var currentAuthToken: String? = null

    fun bindUser(uid: String, token: String) {
        currentUserUid = uid
        currentAuthToken = token
    }

    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()

    suspend fun pushState(state: EmotionalState) = withContext(Dispatchers.IO) {
        val uid = currentUserUid ?: return@withContext
        val token = currentAuthToken ?: return@withContext

        // Use PostgREST to patch the users table where id = uid
        val dynamicUrl = "$SUPABASE_URL/rest/v1/users?id=eq.$uid"
        val currentTime = Instant.now().toEpochMilli()

        try {
            // Updated JSON body targeting the widget_data JSONB column
            val jsonBody = """
            {
              "widget_data": {
                "active": ${state.active},
                "revenge": ${state.revenge},
                "fomo": ${state.fomo},
                "mistake": ${state.mistake},
                "ego": ${state.ego},
                "confidence": ${state.confidence},
                "focus": ${state.focus},
                "probability": ${state.probability},
                "timestamp": $currentTime
              }
            }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(dynamicUrl))
                .header("Content-Type", "application/json")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() >= 400) {
                println("Supabase Push Error: ${response.statusCode()} ${response.body()}")
            } else {
                println("Supabase Push Success: ${response.statusCode()}")
            }

            if (response.statusCode() in 200..299) {
                println("Supabase Sync: SUCCESS (${state.active})")
            } else {
                println("Supabase Sync: FAILED - ${response.body()}")
            }
        } catch (e: Exception) {
            println("Supabase Sync: ERROR - ${e.message}")
        }
    }

    suspend fun fetchDeskSnapshot(): DeskSnapshot = withContext(Dispatchers.IO) {
        val uid = currentUserUid ?: return@withContext DeskSnapshot()
        val token = currentAuthToken ?: return@withContext DeskSnapshot()

        try {
            val tradesUrl =
                "$SUPABASE_URL/rest/v1/trades?user_id_uuid=eq.$uid" +
                    "&status=in.(ACTIVE,CLOSED,WATCHLIST,SKIPPED)" +
                    "&select=status,pnl,created_at,exit_timestamp" +
                    "&order=created_at.desc&limit=200"
            val tradesBody = get(tradesUrl, token)
            val userUrl = "$SUPABASE_URL/rest/v1/users?id=eq.$uid&select=cooldown_until,settings,circuitBreakerFreeze"
            val userBody = get(userUrl, token)

            val todayStart = startOfLocalDayMs()
            var todayPnl = 0.0
            var openCount = 0
            var watchCount = 0
            var closedToday = 0
            var winsToday = 0
            var lossesToday = 0

            for (obj in parseJsonObjectArray(tradesBody)) {
                val status = jsonString(obj, "status") ?: continue
                val pnl = jsonNumber(obj, "pnl") ?: 0.0
                when (status) {
                    "ACTIVE" -> openCount++
                    "WATCHLIST" -> watchCount++
                    "CLOSED" -> {
                        val exitMs = parseTimeMs(jsonRaw(obj, "exitTimestamp"))
                            ?: parseTimeMs(jsonRaw(obj, "exit_timestamp"))
                            ?: parseTimeMs(jsonRaw(obj, "created_at"))
                            ?: 0L
                        if (exitMs >= todayStart) {
                            closedToday++
                            todayPnl += pnl
                            if (pnl > 0) winsToday++ else if (pnl < 0) lossesToday++
                        }
                    }
                }
            }

            val userObj = parseJsonObjectArray(userBody).firstOrNull().orEmpty()
            val settingsBlob = extractNestedObject(userObj, "settings") ?: userObj
            val currency = jsonString(settingsBlob, "currencySymbol") ?: "₹"
            val zen = jsonBool(settingsBlob, "zenMode") ?: false
            val lossLimit = jsonNumber(settingsBlob, "dailyLossLimit")
            val cooldown = parseTimeMs(jsonRaw(userObj, "cooldown_until"))
                ?: parseTimeMs(jsonRaw(userObj, "circuitBreakerFreeze"))

            DeskSnapshot(
                todayPnl = todayPnl,
                openCount = openCount,
                watchCount = watchCount,
                closedToday = closedToday,
                winsToday = winsToday,
                lossesToday = lossesToday,
                currencySymbol = currency,
                zenMode = zen,
                cooldownUntilMs = cooldown,
                dailyLossLimit = lossLimit,
                lastSyncOk = true
            )
        } catch (e: Exception) {
            println("Desk snapshot: ERROR - ${e.message}")
            DeskSnapshot()
        }
    }

    private fun get(url: String, token: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("apikey", SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 400) {
            println("Supabase GET Error: ${response.statusCode()} ${response.body()}")
            return "[]"
        }
        return response.body()
    }
}

private fun startOfLocalDayMs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun parseJsonObjectArray(body: String): List<String> {
    val start = body.indexOf('[')
    val end = body.lastIndexOf(']')
    if (start < 0 || end <= start) {
        val objStart = body.indexOf('{')
        val objEnd = body.lastIndexOf('}')
        return if (objStart >= 0 && objEnd > objStart) listOf(body.substring(objStart, objEnd + 1)) else emptyList()
    }
    val inner = body.substring(start + 1, end)
    val result = mutableListOf<String>()
    var depth = 0
    var begin = -1
    var inStr = false
    var escape = false
    for (i in inner.indices) {
        val c = inner[i]
        if (inStr) {
            if (escape) escape = false
            else if (c == '\\') escape = true
            else if (c == '"') inStr = false
            continue
        }
        when (c) {
            '"' -> inStr = true
            '{' -> {
                if (depth == 0) begin = i
                depth++
            }
            '}' -> {
                depth--
                if (depth == 0 && begin >= 0) {
                    result.add(inner.substring(begin, i + 1))
                    begin = -1
                }
            }
        }
    }
    return result
}

private fun extractNestedObject(parent: String, key: String): String? {
    val needle = "\"$key\""
    val idx = parent.indexOf(needle)
    if (idx < 0) return null
    val colon = parent.indexOf(':', idx + needle.length)
    if (colon < 0) return null
    var i = colon + 1
    while (i < parent.length && parent[i].isWhitespace()) i++
    if (i >= parent.length || parent[i] != '{') return null
    var depth = 0
    var inStr = false
    var escape = false
    val begin = i
    while (i < parent.length) {
        val c = parent[i]
        if (inStr) {
            if (escape) escape = false
            else if (c == '\\') escape = true
            else if (c == '"') inStr = false
            i++
            continue
        }
        when (c) {
            '"' -> inStr = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return parent.substring(begin, i + 1)
            }
        }
        i++
    }
    return null
}

private fun jsonString(obj: String, key: String): String? {
    val re = """"$key"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
    return re.find(obj)?.groupValues?.get(1)
}

private fun jsonNumber(obj: String, key: String): Double? {
    if (Regex(""""$key"\s*:\s*null""").containsMatchIn(obj)) return null
    val re = """"$key"\s*:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)""".toRegex()
    return re.find(obj)?.groupValues?.get(1)?.toDoubleOrNull()
}

private fun jsonBool(obj: String, key: String): Boolean? {
    val re = """"$key"\s*:\s*(true|false)""".toRegex()
    return re.find(obj)?.groupValues?.get(1)?.toBooleanStrictOrNull()
}

private fun jsonRaw(obj: String, key: String): String? {
    val str = jsonString(obj, key)
    if (str != null) return str
    val num = jsonNumber(obj, key)
    if (num != null) {
        val asLong = num.toLong()
        return if (asLong.toDouble() == num) asLong.toString() else num.toString()
    }
    return null
}

private fun parseTimeMs(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    raw.toLongOrNull()?.let { v ->
        return if (v < 1_000_000_000_000L) v * 1000 else v
    }
    raw.toDoubleOrNull()?.let { v ->
        val n = v.toLong()
        return if (n < 1_000_000_000_000L) n * 1000 else n
    }
    return try {
        Instant.parse(raw).toEpochMilli()
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
