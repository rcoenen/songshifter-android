package com.musicredirector

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ClipboardManager
import android.content.ClipData
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.app.SearchManager
import java.net.URLEncoder

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
                    MusicLinkExtractor.extractSongInfo(this@RedirectActivity, url)
                }
                
                if (songInfo != null) {
                    val searchUrl = MusicLinkExtractor.buildSearchUrl(songInfo, targetPlatform)
                    
                    Log.d(TAG, "Redirecting to: $searchUrl")
                    
                    // For YouTube Music, use a more compatible URL format
                    if (targetPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                        // Get clean search query
                        val cleanedTitle = songInfo.title.trim()
                        val cleanedArtist = songInfo.artist.trim()
                        val searchQuery = "$cleanedTitle $cleanedArtist"
                        val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                        
                        // YouTube Music URL - using deep link format
                        val ytMusicURL = "https://music.youtube.com/search?q=$encodedQuery"
                        
                        Log.d(TAG, "Redirecting to YouTube Music. Song: '$cleanedTitle', Artist: '$cleanedArtist'")
                        Log.d(TAG, "YouTube Music URL: $ytMusicURL")
                        
                        try {
                            // Save search terms to clipboard for easy manual search if needed
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Song Search", searchQuery)
                            clipboard.setPrimaryClip(clip)
                            
                            // Show helpful toast
                            Toast.makeText(this@RedirectActivity,
                                "Opening YouTube Music. Search terms copied to clipboard.",
                                Toast.LENGTH_LONG).show()
                            
                            // APPROACH 1: Try direct activity targeting
                            val searchUrl = "https://music.youtube.com/search?q=$encodedQuery"
                            
                            val directIntent = Intent(Intent.ACTION_VIEW).apply {
                                setData(Uri.parse(searchUrl))
                                setClassName(
                                    "com.google.android.apps.youtube.music",
                                    "com.google.android.apps.youtube.music.activities.MusicSearchActivity"
                                )
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            
                            Log.d(TAG, "Attempting direct activity targeting for YouTube Music")
                            try {
                                startActivity(directIntent)
                                Log.d(TAG, "Successfully launched YouTube Music search activity")
                                finish()
                                return@launch
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to launch YouTube Music search activity: ${e.message}")
                                e.printStackTrace()
                            }
                            
                            // APPROACH 2: Try YouTube Music app with encoded URL
                            val ytMusicIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                                setPackage("com.google.android.apps.youtube.music")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            
                            Log.d(TAG, "Checking if YouTube Music can handle encoded URL")
                            val ytMusicResolveInfo = ytMusicIntent.resolveActivity(packageManager)
                            if (ytMusicResolveInfo != null) {
                                Log.d(TAG, "YouTube Music found, launching with encoded URL")
                                try {
                                    startActivity(ytMusicIntent)
                                    Log.d(TAG, "Successfully launched YouTube Music with encoded URL")
                                    finish()
                                    return@launch
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to launch YouTube Music with encoded URL: ${e.message}")
                                    e.printStackTrace()
                                }
                            } else {
                                Log.d(TAG, "YouTube Music cannot handle encoded URL")
                            }
                            
                            // APPROACH 3: Browser fallback with encoded URL
                            Log.d(TAG, "Falling back to browser launch with encoded URL")
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                            startActivity(browserIntent)
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening YouTube Music: ${e.message}")
                            showErrorAndExit("Error opening YouTube Music app.")
                        }
                    } 
                    else if (targetPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                        // Handle Spotify redirection
                        try {
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
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening Spotify: ${e.message}")
                            showErrorAndExit("Error opening Spotify app.")
                        }
                    } else {
                        // Default case
                        openUrl(searchUrl)
                    }
                } else {
                    // Song info extraction failed 
                    Log.e(TAG, "Failed to extract song information from URL: $url")
                    
                    // Show error message instead of proceeding with a generic fallback
                    showErrorAndExit("Unable to extract song title from link. Redirection cancelled.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in redirectUrl: ${e.message}")
                e.printStackTrace()
                showErrorAndExit("An error occurred during redirection.")
            }
        }
    }
    
    private fun showErrorAndExit(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Delay finishing the activity to ensure the toast is visible
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 3000)
    }
    
    private fun openUrl(url: String) {
        try {
            Log.d(TAG, "Opening URL: $url")
            
            // Handle YouTube Music URLs specially
            if (url.contains("music.youtube.com")) {
                // First try to open in YouTube Music app directly
                val ytMusicIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                ytMusicIntent.setPackage("com.google.android.apps.youtube.music")
                
                if (ytMusicIntent.resolveActivity(packageManager) != null) {
                    Log.d(TAG, "Opening in YouTube Music app")
                    startActivity(ytMusicIntent)
                    return
                }
                
                // Second, try with Chrome or another browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                // Try to use a full browser that can handle music.youtube.com
                val browserPackageName = getBrowserPackageName()
                if (browserPackageName != null) {
                    browserIntent.setPackage(browserPackageName)
                    Log.d(TAG, "Opening YouTube Music URL in browser: $browserPackageName")
                }
                
                startActivity(browserIntent)
            } else {
                // Regular intent for other URLs
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, R.string.error_opening_url, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    // Try to find an installed browser package
    private fun getBrowserPackageName(): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
        val resolveInfo = packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        
        // Known browser packages
        val browsers = arrayOf(
            "com.android.chrome",  // Chrome
            "com.google.android.apps.chrome", // Chrome
            "org.mozilla.firefox", // Firefox
            "com.opera.browser",   // Opera
            "com.microsoft.emmx"   // Edge
        )
        
        // First check if there's a default browser set
        if (resolveInfo != null) {
            val defaultBrowser = resolveInfo.activityInfo.packageName
            Log.d(TAG, "Default browser: $defaultBrowser")
            
            // Don't return our own package
            if (defaultBrowser != packageName) {
                return defaultBrowser
            }
        }
        
        // If no default or default is our app, try common browsers
        for (browser in browsers) {
            try {
                packageManager.getPackageInfo(browser, 0)
                return browser // Found an installed browser
            } catch (e: Exception) {
                // Browser not installed
            }
        }
        
        return null // No suitable browser found
    }
} 