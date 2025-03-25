package com.musicredirector

import android.content.Context
import android.util.Log

/**
 * Delegates extraction to platform-specific extractors.
 * Each platform (YouTube Music, Spotify, Shazam) has its own dedicated extraction logic.
 */
class WebViewExtractor(private val context: Context) {
    private val TAG = "WebViewExtractor"
    private val youtubeMusicExtractor by lazy { YouTubeMusicExtractor(context) }

    suspend fun extractFromYouTubeMusic(url: String): SongInfo? {
        Log.d(TAG, "Delegating to YouTube Music extractor")
        return youtubeMusicExtractor.extract(url)
    }
} 