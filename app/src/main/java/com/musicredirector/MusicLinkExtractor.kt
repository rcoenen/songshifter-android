package com.musicredirector

import android.net.Uri
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class MusicLinkExtractor {
    companion object {
        private const val TAG = "MusicLinkExtractor"
        private const val TIMEOUT_MS = 10000
        
        /**
         * Extract song title and artist from various music platform links
         */
        suspend fun extractSongInfo(url: String): SongInfo? {
            return when {
                url.contains("open.spotify.com/track") -> extractFromSpotify(url)
                url.contains("music.youtube.com/watch") -> extractFromYouTubeMusic(url)
                url.contains("shazam.com/song") -> extractFromShazam(url)
                else -> null
            }
        }
        
        private suspend fun extractFromSpotify(url: String): SongInfo? {
            Log.d(TAG, "Extracting from Spotify URL: $url")
            return try {
                val doc = Jsoup.connect(url).timeout(TIMEOUT_MS).get()
                val title = doc.title()
                Log.d(TAG, "Spotify page title: $title")
                
                // Spotify page titles are formatted as "Song Name - Artist | Spotify"
                val parts = title.split(" - ")
                if (parts.size >= 2) {
                    val songTitle = parts[0].trim()
                    val artist = parts[1].split(" | ")[0].trim()
                    Log.d(TAG, "Extracted from Spotify - Title: \"$songTitle\", Artist: \"$artist\"")
                    SongInfo(songTitle, artist)
                } else {
                    // Fallback to just using the page title as query
                    Log.d(TAG, "Fallback - using full title: $title")
                    val cleanTitle = title.replace(" | Spotify", "").trim()
                    SongInfo(cleanTitle, "")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error extracting Spotify info: ${e.message}")
                e.printStackTrace()
                // Fallback - just use the track ID
                val trackId = url.substringAfterLast("/").split("?")[0]
                Log.d(TAG, "Using track ID as fallback: $trackId")
                SongInfo("spotify track $trackId", "")
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
                // Fallback - extract video ID from URL
                val videoId = Uri.parse(url).getQueryParameter("v") ?: ""
                Log.d(TAG, "Using video ID as fallback: $videoId")
                SongInfo("youtube music video $videoId", "")
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
                    // Fallback to page title
                    val pageTitle = doc.title().replace("| Shazam", "").trim()
                    Log.d(TAG, "Shazam fallback - using page title: $pageTitle")
                    SongInfo(pageTitle, "")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error extracting Shazam info: ${e.message}")
                e.printStackTrace()
                // Fallback - extract song ID from URL
                val songId = url.substringAfterLast("/").split("?")[0]
                Log.d(TAG, "Using song ID as fallback: $songId")
                SongInfo("shazam song $songId", "")
            }
        }
        
        /**
         * Build a search URL for the target platform using song info
         */
        fun buildSearchUrl(songInfo: SongInfo, targetPlatform: String): String {
            val query = Uri.encode("${songInfo.title} ${songInfo.artist}".trim())
            Log.d(TAG, "Building search URL for platform: $targetPlatform with query: $query")
            
            return when (targetPlatform) {
                PreferencesHelper.PLATFORM_SPOTIFY -> {
                    // Spotify deep link for Android
                    "spotify:search:$query"
                }
                PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> {
                    // This URL is for browser fallback - app intent will use a different format
                    // Using regular youtube search format which works better in browsers
                    "https://www.youtube.com/results?search_query=$query"
                }
                else -> {
                    // Default to Spotify
                    "spotify:search:$query"
                }
            }
        }
    }
    
    data class SongInfo(val title: String, val artist: String)
} 