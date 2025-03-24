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

/**
 * Extracts song information from various music platform URLs.
 * Each platform has a single, robust extraction method without fallbacks.
 *
 * Supported platforms:
 * - Spotify (via oEmbed API)
 * - YouTube Music (via WebView)
 * - Shazam (via meta tags)
 *
 * @property context Android context required for WebView operations
 */
class MusicLinkExtractor(private val context: Context) {
    companion object {
        private const val TAG = "MusicLinkExtractor"
        private const val TIMEOUT_MS = 10000
        
        /**
         * Extracts song title and artist from various music platform links.
         * Uses platform-specific extractors based on the URL pattern.
         *
         * @param context Android context required for extraction
         * @param url The music platform URL to extract from
         * @return SongInfo if extraction succeeds, null otherwise
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
         * Builds a search URL for the target platform using song info.
         *
         * @param songInfo The song information to search for
         * @param targetPlatform The platform to build the search URL for
         * @return A search URL for the specified platform
         */
        fun buildSearchUrl(songInfo: SongInfo, targetPlatform: String): String {
            // If we have both title and artist, use them
            val searchQuery = if (songInfo.artist.isNotBlank()) {
                "${songInfo.title} ${songInfo.artist}"
            } else {
                // If we only have title, use it alone but add "song" to improve results
                "${songInfo.title} song"
            }
            val encodedQuery = Uri.encode(searchQuery.trim())
            
            return when (targetPlatform) {
                PreferencesHelper.PLATFORM_SPOTIFY -> "https://open.spotify.com/search/$encodedQuery"
                PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$encodedQuery"
                else -> "https://music.youtube.com/search?q=$encodedQuery" // Default to YouTube Music
            }
        }
    }
    
    /**
     * Extracts song information from a Spotify track URL using their embed page.
     * This is the most reliable method as it:
     * 1. Uses the public embed page which is designed for stability
     * 2. Doesn't require API tokens or authentication
     * 3. Has consistent HTML structure with h1 (title) and h2 (artist)
     * 4. Runs network call on background thread to avoid ANR
     *
     * @param url The Spotify track URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    private suspend fun extractFromSpotify(url: String): SongInfo? {
        Log.d(TAG, "=== Starting Spotify extraction ===")
        Log.d(TAG, "Input URL: $url")
        
        try {
            // Validate Spotify URL format
            if (!url.contains("/track/")) {
                Log.e(TAG, "Invalid Spotify URL format - must be a track URL")
                return null
            }
            
            // Get the track ID and create embed URL
            val trackId = url.substringAfter("/track/").substringBefore("?")
            val embedUrl = "https://open.spotify.com/embed/track/$trackId"
            Log.d(TAG, "Requesting embed URL: $embedUrl")
            
            // Use Jsoup to parse the embed page (on background thread)
            val doc = withContext(Dispatchers.IO) {
                Jsoup.connect(embedUrl)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0")
                    .get()
            }
            
            // Extract title and artist from the page
            val songTitle = doc.select("h1").firstOrNull()?.text()
            val artist = doc.select("h2").firstOrNull()?.text()
            
            Log.d(TAG, "Extracted title: \"$songTitle\"")
            Log.d(TAG, "Extracted artist: \"$artist\"")
            
            if (songTitle.isNullOrBlank() || artist.isNullOrBlank()) {
                Log.e(TAG, "Invalid song info - title or artist is blank")
                return null
            }
            
            Log.d(TAG, "=== Extraction successful ===")
            Log.d(TAG, "Song title: \"$songTitle\"")
            Log.d(TAG, "Artist: \"$artist\"")
            return SongInfo(songTitle, artist)
            
        } catch (e: Exception) {
            Log.e(TAG, "=== Extraction failed ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Extracts song information from a YouTube Music URL using WebView.
     * This method is necessary because YouTube Music requires JavaScript execution.
     *
     * @param url The YouTube Music URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    private suspend fun extractFromYouTubeMusic(url: String): SongInfo? {
        Log.d(TAG, "=== Starting YouTube Music extraction ===")
        Log.d(TAG, "Input URL: $url")
        
        try {
            // Validate YouTube Music URL format
            if (!url.contains("/watch")) {
                Log.e(TAG, "Invalid YouTube Music URL format - must be a watch URL")
                return null
            }
            
            Log.d(TAG, "Using WebView extraction")
            val webViewExtractor = WebViewExtractor(context)
            val result = withContext(Dispatchers.Main) {
                webViewExtractor.extractFromYouTubeMusic(url)
            }
            
            if (result != null) {
                Log.d(TAG, "=== Extraction successful ===")
                Log.d(TAG, "Song title: \"${result.title}\"")
                Log.d(TAG, "Artist: \"${result.artist}\"")
                return result
            }
            
            Log.e(TAG, "WebView extraction failed to extract song info")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "=== Extraction failed ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Extracts song information from a Shazam URL using meta tags.
     * Uses og:title meta tag as the most reliable source of information.
     *
     * @param url The Shazam song URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    private suspend fun extractFromShazam(url: String): SongInfo? {
        Log.d(TAG, "=== Starting Shazam extraction ===")
        Log.d(TAG, "Input URL: $url")
        
        return try {
            // Validate Shazam URL format
            if (!url.contains("/song/")) {
                Log.e(TAG, "Invalid Shazam URL format - must be a song URL")
                return null
            }
            
            Log.d(TAG, "Fetching Shazam page content...")
            val doc = Jsoup.connect(url)
                .timeout(TIMEOUT_MS)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            
            // Extract from meta tags - this is the most reliable method
            val metaTitle = doc.select("meta[property=og:title]").attr("content")
            Log.d(TAG, "Meta title content: \"$metaTitle\"")
            
            if (metaTitle.isBlank()) {
                Log.e(TAG, "No meta title found")
                return null
            }
            
            if (!metaTitle.contains(" - ")) {
                Log.e(TAG, "Meta title does not contain artist separator: \"$metaTitle\"")
                return null
            }
            
            val parts = metaTitle.split(" - ", limit = 2)  // Only split on first occurrence
            val title = parts[0].trim()
            val artist = parts[1].trim()
            
            // Validate extracted data
            if (title.isBlank() || artist.isBlank()) {
                Log.e(TAG, "Invalid song info - title or artist is blank after splitting")
                return null
            }
            
            Log.d(TAG, "=== Extraction successful ===")
            Log.d(TAG, "Song title: \"$title\"")
            Log.d(TAG, "Artist: \"$artist\"")
            SongInfo(title, artist)
            
        } catch (e: Exception) {
            Log.e(TAG, "=== Extraction failed ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:")
            e.printStackTrace()
            null
        }
    }
} 