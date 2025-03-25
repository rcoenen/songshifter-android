package com.songshifter.extractors

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.songshifter.SongInfo

/**
 * Dedicated extractor for Shazam links.
 * Uses meta tags from Shazam pages to extract song information.
 */
class ShazamExtractor(private val context: Context) {
    companion object {
        private const val TAG = "ShazamExtractor"
        private const val TIMEOUT_MS = 15000
    }
    
    /**
     * Extracts song information from a Shazam URL using meta tags.
     * Uses og:title meta tag as the most reliable source of information.
     *
     * @param url The Shazam song URL
     * @return SongInfo if extraction succeeds, null if any validation fails
     */
    suspend fun extract(url: String): SongInfo? {
        Log.d(TAG, "=== Starting Shazam extraction ===")
        Log.d(TAG, "Input URL: $url")
        
        return try {
            // Validate Shazam URL format
            if (!url.contains("/song/")) {
                Log.e(TAG, "Invalid Shazam URL format - must be a song URL")
                return null
            }
            
            Log.d(TAG, "Fetching Shazam page content...")
            val doc = withContext(Dispatchers.IO) {
                Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()
            }
            
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