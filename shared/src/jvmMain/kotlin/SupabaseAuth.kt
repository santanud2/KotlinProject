package com.example.widget

import java.net.HttpURLConnection
import java.net.URL
import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseAuth {
    // TODO: Replace with your actual Supabase URL (e.g. https://xyz.supabase.co)
    private const val SUPABASE_URL = "https://fbprttgoplqwtmziyium.supabase.co"
    // TODO: Replace with your actual Supabase Anon Key
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZicHJ0dGdvcGxxd3Rteml5aXVtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc2NDMyOTQsImV4cCI6MjEwMzIxOTI5NH0.juhzSqgZLxcQ_9v7jhLLauTWO8uC7unZGW1hEwa7USE"
    
    // Native desktop preferences store
    private val prefs = Preferences.userRoot().node("com.example.widget.auth")

    fun getSavedCredentials(): Pair<String, String>? {
        val email = prefs.get("email", null)
        val pass = prefs.get("pass", null)
        if (email != null && pass != null) return Pair(email, pass)
        return null
    }

    fun clearCredentials() {
        prefs.remove("email")
        prefs.remove("pass")
    }

    // Returns the UID and Token if successful, or null if login fails
    suspend fun login(email: String, pass: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
            connection.doOutput = true

            // Send Email and Password
            val payload = """{"email":"$email","password":"$pass"}"""
            connection.outputStream.write(payload.toByteArray())

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                
                // Parse Supabase response for access_token and user.id
                val uidRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
                val tokenRegex = """"access_token"\s*:\s*"([^"]+)"""".toRegex()

                val uid = uidRegex.find(response)?.groupValues?.get(1)
                val token = tokenRegex.find(response)?.groupValues?.get(1)

                if (uid != null && token != null) {
                    prefs.put("email", email)
                    prefs.put("pass", pass)
                    return@withContext Pair(uid, token)
                }
                return@withContext null
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                println("SUPABASE AUTH REJECTED: $errorResponse")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
