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
     * Extracts song information from a Spotify track URL.
     * Delegates to the SpotifyExtractor for platform-specific implementation.
     *
     * @param url The Spotify track URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    private suspend fun extractFromSpotify(url: String): SongInfo? {
        Log.d(TAG, "=== Starting Spotify extraction via WebViewExtractor ===")
        
        try {
            val webViewExtractor = WebViewExtractor(context)
            return webViewExtractor.extractFromSpotify(url)
        } catch (e: Exception) {
            Log.e(TAG, "Error delegating to SpotifyExtractor: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Extracts song information from a YouTube Music URL.
     * Delegates to the YouTubeMusicExtractor for platform-specific implementation.
     *
     * @param url The YouTube Music URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    private suspend fun extractFromYouTubeMusic(url: String): SongInfo? {
        Log.d(TAG, "=== Starting YouTube Music extraction via WebViewExtractor ===")
        
        try {
            val webViewExtractor = WebViewExtractor(context)
            return withContext(Dispatchers.Main) {
                webViewExtractor.extractFromYouTubeMusic(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error delegating to YouTubeMusicExtractor: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Extracts song information from a Shazam URL.
     * Delegates to the ShazamExtractor for platform-specific implementation.
     *
     * @param url The Shazam song URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    private suspend fun extractFromShazam(url: String): SongInfo? {
        Log.d(TAG, "=== Starting Shazam extraction via WebViewExtractor ===")
        
        try {
            val webViewExtractor = WebViewExtractor(context)
            return webViewExtractor.extractFromShazam(url)
        } catch (e: Exception) {
            Log.e(TAG, "Error delegating to ShazamExtractor: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
} 