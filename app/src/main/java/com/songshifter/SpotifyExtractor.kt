package com.songshifter.extractors

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.songshifter.SongInfo

/**
 * Dedicated extractor for Spotify links.
 * Uses Spotify's embed page which provides stable and reliable extraction.
 */
class SpotifyExtractor(private val context: Context) {
    companion object {
        private const val TAG = "SpotifyExtractor"
        private const val TIMEOUT_MS = 15000
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
    suspend fun extract(url: String): SongInfo? {
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
} 