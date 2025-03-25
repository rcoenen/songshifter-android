package com.songshifter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Handles redirecting URLs between music platforms
 */
class RedirectionHandler(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper
) {
    private val TAG = "RedirectionHandler"
    
    // Example URLs for testing
    private val EXAMPLE_SPOTIFY_URL = "https://open.spotify.com/track/6ciGSCeUiA46HANRzcq8o0?si=hNwYN8OsReahI3JecgRaFg"
    private val EXAMPLE_YOUTUBE_MUSIC_URL = "https://music.youtube.com/watch?v=BsoaesiWaCo&si=W2A3SeZGupYgMApr"
    
    // Package names
    private val SPOTIFY_PACKAGE = "com.spotify.music"
    private val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    
    /**
     * Performs a test redirection based on the provided URL
     */
    fun testRedirection(testUrl: String) {
        Log.d(TAG, "⚙️ Starting redirection for URL: $testUrl")
        
        try {
            // Check if URL contains essential parameters for YouTube Music or Spotify
            if (testUrl.contains("music.youtube.com") && !testUrl.contains("watch?v=")) {
                Log.e(TAG, "❌ YouTube Music URL missing watch?v= parameter: $testUrl")
                Toast.makeText(context, "Invalid YouTube Music URL format", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (testUrl.contains("open.spotify.com") && !testUrl.contains("/track/")) {
                Log.e(TAG, "❌ Spotify URL missing /track/ parameter: $testUrl")
                Toast.makeText(context, "Invalid Spotify URL format", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create and start redirection intent
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testUrl))
            intent.setPackage(context.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "✅ Redirection intent started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during redirection: ${e.message}", e)
            Toast.makeText(context, "Failed to process link: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Tests redirection based on current platform settings
     */
    fun testPlatformRedirection() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        Log.d(TAG, "📱 Test redirection with preferred platform: $preferredPlatform")
        
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            testRedirection(EXAMPLE_YOUTUBE_MUSIC_URL)
        } else {
            testRedirection(EXAMPLE_SPOTIFY_URL)
        }
    }
    
    /**
     * Handles an incoming intent with a URI
     * Redirects to the appropriate app based on the URI and preferred platform
     */
    fun handleIncomingUri(uri: Uri) {
        val urlString = uri.toString()
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        Log.d(TAG, "🔀 Handling URI: $urlString (preferred platform: $preferredPlatform)")
        
        // Determine source platform based on URL
        val isYouTubeMusicUrl = urlString.contains("music.youtube.com")
        val isSpotifyUrl = urlString.contains("open.spotify.com")
        
        // Log detailed URL analysis
        Log.d(TAG, "📊 URL Analysis: isYouTubeMusicUrl=$isYouTubeMusicUrl, isSpotifyUrl=$isSpotifyUrl")
        
        if (isYouTubeMusicUrl) {
            Log.d(TAG, "🎵 YouTube Music URL parameters: " + Uri.parse(urlString).getQueryParameter("v"))
        } else if (isSpotifyUrl) {
            val trackId = urlString.split("/track/").getOrNull(1)?.split("?")?.getOrNull(0)
            Log.d(TAG, "🎵 Spotify Track ID: $trackId")
        }
        
        // Only redirect if the source platform doesn't match the preferred platform
        // Otherwise relay to the native app
        if (isYouTubeMusicUrl) {
            if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                // Redirect YouTube Music to our redirect activity
                Log.d(TAG, "🔀 Redirecting YouTube Music URL to our extraction service")
                testRedirection(urlString)
            } else {
                // Relay back to YouTube Music app
                Log.d(TAG, "🔀 Relaying YouTube Music URL to YouTube Music app")
                relayToNativeApp(urlString, YOUTUBE_MUSIC_PACKAGE)
            }
        } else if (isSpotifyUrl) {
            if (preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                // Redirect Spotify to our redirect activity
                Log.d(TAG, "🔀 Redirecting Spotify URL to our extraction service")
                testRedirection(urlString)
            } else {
                // Relay back to Spotify app
                Log.d(TAG, "🔀 Relaying Spotify URL to Spotify app")
                relayToNativeApp(urlString, SPOTIFY_PACKAGE)
            }
        } else {
            // Unknown URL format, just try our redirection
            Log.d(TAG, "🔀 Unknown URL format, attempting standard redirection")
            testRedirection(urlString)
        }
    }
    
    /**
     * Relays a URL to its native app
     */
    private fun relayToNativeApp(url: String, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setPackage(packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "✓ Successfully relayed to $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error relaying to $packageName: ${e.message}")
            // Fallback to system handler if app isn't installed
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
                Log.d(TAG, "✓ Fallback: opened with system handler")
            } catch (e2: Exception) {
                Log.e(TAG, "✗ Complete failure handling URL: ${e2.message}")
            }
        }
    }
} 