package com.musicredirector

import android.content.Context
import android.net.Uri
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicLinkExtractor(private val context: Context) {
    companion object {
        private const val TAG = "MusicLinkExtractor"
        private const val TIMEOUT_MS = 10000
        
        /**
         * Extract song title and artist from various music platform links
         */
        suspend fun extractSongInfo(context: Context, url: String): SongInfo? {
            val extractor = MusicLinkExtractor(context)
            return when {
                url.contains("open.spotify.com/track") -> extractor.extractFromSpotify(url)
                url.contains("music.youtube.com/watch") -> extractor.extractFromYouTubeMusic(url)
                url.contains("shazam.com/song") -> extractor.extractFromShazam(url)
                else -> null
            }
        }
        
        /**
         * Build a search URL for the target platform
         */
        fun buildSearchUrl(songInfo: SongInfo, targetPlatform: String): String {
            val searchQuery = "${songInfo.title} ${songInfo.artist}".trim()
            val encodedQuery = Uri.encode(searchQuery)
            
            return when (targetPlatform) {
                PreferencesHelper.PLATFORM_SPOTIFY -> "https://open.spotify.com/search/$encodedQuery"
                PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$encodedQuery"
                else -> "https://music.youtube.com/search?q=$encodedQuery" // Default to YouTube Music
            }
        }
    }
    
    private suspend fun extractFromSpotify(url: String): SongInfo? {
        Log.d(TAG, "Extracting from Spotify URL: $url")
        
        // Use Spotify's oEmbed endpoint which requires no auth
        try {
            val oembedUrl = "https://open.spotify.com/oembed?url=$url"
            
            // Make a direct HTTP request to the oEmbed endpoint
            val connection = URL(oembedUrl).openConnection()
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "MusicRedirector Android App")
            
            // Read the response
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            Log.d(TAG, "Spotify oEmbed response: $response")
            
            // Parse JSON
            val json = JSONObject(response)
            val title = json.getString("title")
            // Try to get artist from title if it contains " - "
            val (songTitle, artist) = if (title.contains(" - ")) {
                val parts = title.split(" - ")
                parts[0].trim() to parts[1].trim()
            } else {
                title to ""
            }
            
            Log.d(TAG, "Extracted from Spotify oEmbed - Title: \"$songTitle\", Artist: \"$artist\"")
            return SongInfo(songTitle, artist)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Spotify info via oEmbed: ${e.message}")
            e.printStackTrace()
            
            // Try alternative method before giving up
            return try {
                // Fallback to Jsoup method
                val doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get()
                
                val title = doc.title()
                Log.d(TAG, "Spotify page title: $title")
                
                // Check if the title contains actual info (not "Unsupported browser")
                if (title.contains("Unsupported browser") || title.isEmpty() || title == "Spotify") {
                    Log.d(TAG, "Spotify page title extraction failed, got: $title")
                    throw IOException("Invalid page title: $title")
                }
                
                // Spotify page titles are formatted as "Song Name - Artist | Spotify"
                val cleanedTitle = title.replace(" | Spotify", "").trim()
                Log.d(TAG, "Extracted title: $cleanedTitle")
                
                SongInfo(cleanedTitle, "")
            } catch (e2: Exception) {
                Log.e(TAG, "All Spotify extraction methods failed: ${e2.message}")
                e2.printStackTrace()
                
                // Final fallback: Try WebView extraction
                try {
                    Log.d(TAG, "Attempting WebView extraction on main thread")
                    val webViewExtractor = WebViewExtractor(context)
                    withContext(Dispatchers.Main) {
                        webViewExtractor.extractFromSpotify(url)
                    }
                } catch (e3: Exception) {
                    Log.e(TAG, "WebView extraction failed: ${e3.message}")
                    e3.printStackTrace()
                    null
                }
            }
        }
    }
    
    private suspend fun extractFromYouTubeMusic(url: String): SongInfo? {
        Log.d(TAG, "Extracting from YouTube Music URL: $url")
        return try {
            val doc = Jsoup.connect(url).timeout(TIMEOUT_MS).get()
            val title = doc.title()
            Log.d(TAG, "YouTube Music page title: $title")
            
            // YouTube Music titles are typically "Song Name - Artist - YouTube Music"
            val parts = title.split(" - ")
            if (parts.size >= 3) {
                val songTitle = parts[0].trim()
                val artist = parts[1].trim()
                Log.d(TAG, "Extracted from YouTube Music - Title: \"$songTitle\", Artist: \"$artist\"")
                SongInfo(songTitle, artist)
            } else if (parts.size == 2) {
                val songTitle = parts[0].trim()
                val artist = parts[1].replace("YouTube Music", "").trim()
                Log.d(TAG, "Extracted from YouTube Music - Title: \"$songTitle\", Artist: \"$artist\"")
                SongInfo(songTitle, artist)
            } else {
                // Fallback to just using the page title as query
                Log.d(TAG, "Fallback - using full title: $title")
                val cleanTitle = title.replace("- YouTube Music", "").trim()
                SongInfo(cleanTitle, "")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error extracting YouTube Music info: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun extractFromShazam(url: String): SongInfo? {
        Log.d(TAG, "Extracting from Shazam URL: $url")
        return try {
            val doc = Jsoup.connect(url).timeout(TIMEOUT_MS).get()
            
            // Try multiple approaches to extract song info, similar to the Chrome extension
            var title = ""
            var artist = ""
            
            // Approach 1: Look for specific heading elements
            title = doc.select("h1").firstOrNull()?.text() ?: ""
            artist = doc.select("h2 a").firstOrNull()?.text() ?: ""
            
            Log.d(TAG, "Shazam extraction approach 1 - Title: \"$title\", Artist: \"$artist\"")
            
            // Approach 2: Try to extract from meta tags
            if (title.isEmpty() || artist.isEmpty()) {
                val metaTitle = doc.select("meta[property=og:title]").attr("content")
                if (metaTitle.contains(" - ")) {
                    val parts = metaTitle.split(" - ")
                    title = parts[0].trim()
                    artist = parts[1].trim()
                    Log.d(TAG, "Shazam extraction approach 2 - Title: \"$title\", Artist: \"$artist\"")
                }
            }
            
            // If we still couldn't find them, try parsing the URL
            if (title.isEmpty()) {
                val urlPath = url.substringAfterLast("/")
                val songSlug = urlPath.split("?")[0]
                // Replace hyphens with spaces and capitalize first letters
                title = songSlug.replace("-", " ")
                Log.d(TAG, "Shazam extraction approach 3 - Title from URL: \"$title\"")
            }
            
            if (title.isNotEmpty()) {
                SongInfo(title, artist)
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error extracting Shazam info: ${e.message}")
            e.printStackTrace()
            null
        }
    }
} 