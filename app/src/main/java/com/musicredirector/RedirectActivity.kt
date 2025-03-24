package com.musicredirector

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RedirectActivity : AppCompatActivity() {
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var statusText: TextView
    private lateinit var progressOverlay: View
    
    companion object {
        private const val TAG = "RedirectActivity"
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redirect)

        statusText = findViewById(R.id.status_text)
        progressOverlay = findViewById(R.id.progress_overlay)

        preferencesHelper = PreferencesHelper(this)
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        val url = intent.data?.toString()
        if (url == null) {
            Log.e(TAG, "No URL provided")
            finish()
            return
        }
        
        Log.d(TAG, "Received URL: $url")
        
        if (url.contains("music.youtube.com") && isYouTubeMusicEnabled()) {
            showYouTubeMusicWarning()
            finish()
            return
        }
        
        if (!shouldRedirect(url)) {
            Log.d(TAG, "Redirection not needed for this URL")
            finish()
            return
        }
        
        processRedirection(url)
    }
    
    private fun processRedirection(url: String) {
        when {
            url.contains("music.youtube.com") -> {
                Log.d(TAG, "YouTube Music URL detected")
                showProgress("Extracting song info...")
                extractAndRedirect(url)
            }
            url.contains("open.spotify.com") -> {
                Log.d(TAG, "Spotify URL detected")
                showProgress("Extracting song info...")
                extractAndRedirect(url)
            }
            else -> {
                Log.d(TAG, "Unknown URL type, finishing")
                finish()
            }
        }
    }
    
    private fun extractAndRedirect(url: String) {
        lifecycleScope.launch {
            try {
                val songInfo = MusicLinkExtractor.extractSongInfo(this@RedirectActivity, url)
                
                if (songInfo != null) {
                    val preferredPlatform = preferencesHelper.getPreferredPlatform()
                    showProgress("Opening ${getPlatformName(preferredPlatform)}...")
                    
                    // Small delay to show the opening message
                    withContext(Dispatchers.IO) {
                        Thread.sleep(500)
                    }
                    
                    val searchUrl = MusicLinkExtractor.buildSearchUrl(songInfo, preferredPlatform)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                        when (preferredPlatform) {
                            PreferencesHelper.PLATFORM_SPOTIFY -> setPackage(SPOTIFY_PACKAGE)
                            PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> setPackage("com.google.android.apps.youtube.music")
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } else {
                    showErrorAndExit("Could not extract song information")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during redirection: ${e.message}")
                showErrorAndExit("Error during redirection")
            } finally {
                finish()
            }
        }
    }
    
    private fun shouldRedirect(url: String): Boolean {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        val allEnabled = preferencesHelper.isAllRedirectionsEnabled()
        
        if (!allEnabled) {
            Log.d(TAG, "All redirections disabled")
            return false
        }
        
        return when {
            url.contains("open.spotify.com/track") -> {
                val result = preferencesHelper.isSpotifyRedirectionEnabled() && 
                            preferredPlatform != PreferencesHelper.PLATFORM_SPOTIFY
                Log.d(TAG, "Spotify URL, should redirect: $result")
                result
            }
            url.contains("music.youtube.com/watch") -> {
                val result = preferencesHelper.isYouTubeMusicRedirectionEnabled() && 
                            preferredPlatform != PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                Log.d(TAG, "YouTube Music URL, should redirect: $result")
                result
            }
            url.contains("shazam.com/song") -> {
                val result = preferencesHelper.isShazamRedirectionEnabled()
                Log.d(TAG, "Shazam URL, should redirect: $result")
                result
            }
            else -> {
                Log.d(TAG, "Unknown URL pattern, no redirect")
                false
            }
        }
    }
    
    private fun showErrorAndExit(message: String) {
        Log.e(TAG, "Error: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun isYouTubeMusicEnabled(): Boolean {
        return try {
            packageManager.getApplicationInfo("com.google.android.apps.youtube.music", 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun showYouTubeMusicWarning() {
        val message = "YouTube Music app is preventing link interception. To use this feature:\n\n" +
                     "1. Go to Settings > Apps\n" +
                     "2. Find 'YouTube Music'\n" +
                     "3. Select 'Disable' or 'Turn off'\n\n" +
                     "This only needs to be done once."
        
        Log.i(TAG, "Showing YouTube Music warning")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Action Required")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:com.google.android.apps.youtube.music")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening settings: ${e.message}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getPlatformName(platform: String): String {
        return when (platform) {
            PreferencesHelper.PLATFORM_SPOTIFY -> "Spotify"
            PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> "YouTube Music"
            else -> "music app"
        }
    }

    private fun showProgress(message: String) {
        statusText.text = message
        progressOverlay.visibility = View.VISIBLE
    }
} 