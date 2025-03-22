package com.musicredirector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RedirectActivity : AppCompatActivity() {
    private lateinit var preferencesHelper: PreferencesHelper
    private val TAG = "RedirectActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesHelper = PreferencesHelper(this)
        Log.d(TAG, "RedirectActivity created - preferences: ${getPreferencesInfo()}")
        
        // Process the intent
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun getPreferencesInfo(): String {
        return "All enabled: ${preferencesHelper.isAllRedirectionsEnabled()}, " +
               "Preferred platform: ${preferencesHelper.getPreferredPlatform()}, " +
               "Spotify redirect: ${preferencesHelper.shouldRedirectSpotify()}, " +
               "YouTube redirect: ${preferencesHelper.shouldRedirectYouTubeMusic()}, " +
               "Shazam redirect: ${preferencesHelper.shouldRedirectShazam()}"
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        
        Log.d(TAG, "Intent received: Action=$action, Data=$data")
        
        // Handle VIEW intent (direct URL)
        if (action == Intent.ACTION_VIEW && data != null) {
            processUrl(data.toString())
            return
        }
        
        // Handle SEND intent (shared URL)
        if (action == Intent.ACTION_SEND) {
            if (intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    Log.d(TAG, "Shared text received: $sharedText")
                    
                    // Extract URL from shared text
                    val urlPattern = Regex("https?://[^\\s]+")
                    val matchResult = urlPattern.find(sharedText)
                    
                    if (matchResult != null) {
                        val extractedUrl = matchResult.value
                        Log.d(TAG, "URL extracted from shared text: $extractedUrl")
                        processUrl(extractedUrl)
                        return
                    }
                }
            }
        }
        
        // Invalid intent, just finish the activity
        Log.e(TAG, "Invalid intent, finishing activity")
        finish()
    }
    
    private fun processUrl(url: String) {
        Log.d(TAG, "Processing URL: $url")
        
        // Show toast for debugging
        Toast.makeText(this, "Processing: $url", Toast.LENGTH_SHORT).show()
        
        // Determine if we should redirect based on source URL and preferences
        if (shouldRedirect(url)) {
            Log.d(TAG, "Redirect condition met, redirecting URL")
            redirectUrl(url)
        } else {
            // Continue with the original URL
            Log.d(TAG, "No redirect needed, opening original URL")
            openUrl(url)
            finish()
        }
    }
    
    private fun shouldRedirect(url: String): Boolean {
        // Get user preferences
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        val allEnabled = preferencesHelper.isAllRedirectionsEnabled()
        
        // Exit early if master toggle is off
        if (!allEnabled) {
            Log.d(TAG, "All redirections disabled")
            return false
        }
        
        // Check if redirection is enabled for the source platform
        val shouldRedirect = when {
            url.contains("open.spotify.com/track") -> {
                // Only redirect Spotify links if Spotify is not the preferred platform
                val result = preferencesHelper.isSpotifyRedirectionEnabled() && 
                             preferredPlatform != PreferencesHelper.PLATFORM_SPOTIFY
                Log.d(TAG, "Spotify URL, should redirect: $result (preferred: $preferredPlatform)")
                result
            }
            url.contains("music.youtube.com/watch") -> {
                // Only redirect YouTube Music links if YouTube Music is not the preferred platform
                val result = preferencesHelper.isYouTubeMusicRedirectionEnabled() && 
                             preferredPlatform != PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                Log.d(TAG, "YouTube Music URL, should redirect: $result (preferred: $preferredPlatform)")
                result
            }
            url.contains("shazam.com/song") -> {
                val result = preferencesHelper.isShazamRedirectionEnabled()
                Log.d(TAG, "Shazam URL, should redirect: $result (preferred: $preferredPlatform)")
                result
            }
            else -> {
                Log.d(TAG, "Unknown URL pattern, no redirect")
                false
            }
        }
        
        return shouldRedirect
    }
    
    private fun redirectUrl(url: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting redirection process")
                
                val targetPlatform = preferencesHelper.getPreferredPlatform()
                Log.d(TAG, "Target platform: $targetPlatform")
                
                Toast.makeText(this@RedirectActivity, 
                    "Redirecting to $targetPlatform...", Toast.LENGTH_SHORT).show()
                
                // Extract song info in background thread
                val songInfo = withContext(Dispatchers.IO) {
                    MusicLinkExtractor.extractSongInfo(url)
                }
                
                if (songInfo != null) {
                    val searchUrl = MusicLinkExtractor.buildSearchUrl(songInfo, targetPlatform)
                    
                    Log.d(TAG, "Redirecting to: $searchUrl")
                    
                    // For YouTube Music, try multiple approaches to launch the app properly
                    if (targetPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                        // Extract clean search query
                        val searchQuery = "${songInfo.title} ${songInfo.artist}".trim()
                        Log.d(TAG, "Using search query: $searchQuery")
                        
                        // Approach 1: Try to launch YouTube Music app with vnd type intent
                        try {
                            val ytMusicAppIntent = Intent(Intent.ACTION_SEARCH)
                                .setPackage("com.google.android.apps.youtube.music")
                                .putExtra("query", searchQuery)
                            
                            Log.d(TAG, "Trying YouTube Music app with ACTION_SEARCH")
                            startActivity(ytMusicAppIntent)
                            finish()
                            return@launch
                        } catch (e: Exception) {
                            Log.e(TAG, "YouTube Music ACTION_SEARCH failed: ${e.message}")
                        }
                        
                        // Approach 2: Try to launch YouTube Music app with direct search URL
                        try {
                            val ytMusicIntent = Intent(Intent.ACTION_VIEW)
                                .setPackage("com.google.android.apps.youtube.music")
                                .setData(Uri.parse("https://music.youtube.com/search?q=${Uri.encode(searchQuery)}"))
                            
                            Log.d(TAG, "Trying YouTube Music with direct URL")
                            startActivity(ytMusicIntent)
                            finish()
                            return@launch
                        } catch (e: Exception) {
                            Log.e(TAG, "YouTube Music direct URL failed: ${e.message}")
                        }
                        
                        // Approach 3: Fall back to regular YouTube app
                        try {
                            val ytIntent = Intent(Intent.ACTION_VIEW)
                                .setPackage("com.google.android.youtube")
                                .setData(Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}"))
                            
                            Log.d(TAG, "Falling back to YouTube app")
                            startActivity(ytIntent)
                            finish()
                            return@launch
                        } catch (e: Exception) {
                            Log.e(TAG, "YouTube fallback failed: ${e.message}")
                        }
                        
                        // Approach 4: Last resort - web browser
                        Log.d(TAG, "Using browser fallback")
                        openUrl("https://music.youtube.com/search?q=${Uri.encode(searchQuery)}")
                    }
                    
                    // For Spotify, try launching Spotify app directly
                    if (targetPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                        val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                        spotifyIntent.setPackage("com.spotify.music")
                        
                        // Check if the Spotify app is installed
                        if (spotifyIntent.resolveActivity(packageManager) != null) {
                            Log.d(TAG, "Launching Spotify app")
                            startActivity(spotifyIntent)
                            finish()
                            return@launch
                        } else {
                            Log.d(TAG, "Spotify app not found, trying web URL")
                            // Fallback to web URL
                            val webUrl = "https://open.spotify.com/search/${Uri.encode("${songInfo.title} ${songInfo.artist}".trim())}"
                            openUrl(webUrl)
                        }
                    } else {
                        // Default case
                        openUrl(searchUrl)
                    }
                } else {
                    // Extraction failed, fall back to original URL
                    Log.e(TAG, "Failed to extract song info, opening original URL")
                    Toast.makeText(this@RedirectActivity, 
                        R.string.error_extracting_info, Toast.LENGTH_SHORT).show()
                    openUrl(url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during redirection: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@RedirectActivity, 
                    R.string.error_redirection_failed, Toast.LENGTH_SHORT).show()
                openUrl(url)
            } finally {
                finish()
            }
        }
    }
    
    private fun openUrl(url: String) {
        try {
            Log.d(TAG, "Opening URL: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: ${e.message}")
            Toast.makeText(this, R.string.error_opening_url, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
} 