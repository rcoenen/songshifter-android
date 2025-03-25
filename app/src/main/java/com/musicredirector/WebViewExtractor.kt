package com.musicredirector

import android.content.Context
import android.util.Log
import com.musicredirector.extractors.YouTubeMusicExtractor
import com.musicredirector.extractors.SpotifyExtractor
import com.musicredirector.extractors.ShazamExtractor

/**
 * Factory class that delegates extraction to platform-specific extractors.
 * Each platform (YouTube Music, Spotify, Shazam) has its own dedicated extraction class
 * with isolated, platform-specific logic.
 */
class WebViewExtractor(private val context: Context) {
    private val TAG = "WebViewExtractor"
    
    // Lazy initialization ensures extractors are only created when needed
    private val youtubeMusicExtractor by lazy { YouTubeMusicExtractor(context) }
    private val spotifyExtractor by lazy { SpotifyExtractor(context) }
    private val shazamExtractor by lazy { ShazamExtractor(context) }

    /**
     * Delegates extraction to the YouTube Music extractor.
     * @param url The YouTube Music URL to extract from
     * @return SongInfo if extraction succeeds, null otherwise
     */
    suspend fun extractFromYouTubeMusic(url: String): SongInfo? {
        Log.d(TAG, "Delegating to YouTubeMusicExtractor")
        return youtubeMusicExtractor.extract(url)
    }
    
    /**
     * Delegates extraction to the Spotify extractor.
     * @param url The Spotify URL to extract from
     * @return SongInfo if extraction succeeds, null otherwise
     */
    suspend fun extractFromSpotify(url: String): SongInfo? {
        Log.d(TAG, "Delegating to SpotifyExtractor")
        return spotifyExtractor.extract(url)
    }
    
    /**
     * Delegates extraction to the Shazam extractor.
     * @param url The Shazam URL to extract from
     * @return SongInfo if extraction succeeds, null otherwise
     */
    suspend fun extractFromShazam(url: String): SongInfo? {
        Log.d(TAG, "Delegating to ShazamExtractor")
        return shazamExtractor.extract(url)
    }
} 