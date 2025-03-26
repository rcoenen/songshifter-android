package com.songshifter.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ReadmeLoader {
    private const val GITHUB_README_URL = "https://raw.githubusercontent.com/rcoenen/songshifter-android/master/README.md"
    private val client = OkHttpClient()

    suspend fun loadReadme(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_README_URL)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch README: ${response.code}"))
            }

            val content = response.body?.string() ?: throw IOException("Empty response body")
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 