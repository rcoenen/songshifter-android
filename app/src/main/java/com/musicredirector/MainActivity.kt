package com.musicredirector

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesHelper: PreferencesHelper
    
    // Status indicators
    private lateinit var allRedirectionsIndicator: View
    private lateinit var spotifyRedirectionIndicator: View
    private lateinit var youtubeRedirectionIndicator: View
    private lateinit var shazamRedirectionIndicator: View
    private lateinit var preferredPlatformText: TextView
    private lateinit var statusText: TextView
    
    // Redirection text labels
    private lateinit var spotifyRedirectText: TextView
    private lateinit var youtubeRedirectText: TextView
    private lateinit var shazamRedirectText: TextView
    
    // Test URL components
    private lateinit var testUrlInput: TextInputEditText
    private lateinit var testUrlButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        preferencesHelper = PreferencesHelper(this)
        
        // Initialize views
        allRedirectionsIndicator = findViewById(R.id.allRedirectionsIndicator)
        spotifyRedirectionIndicator = findViewById(R.id.spotifyRedirectionIndicator)
        youtubeRedirectionIndicator = findViewById(R.id.youtubeRedirectionIndicator)
        shazamRedirectionIndicator = findViewById(R.id.shazamRedirectionIndicator)
        preferredPlatformText = findViewById(R.id.preferredPlatformText)
        statusText = findViewById(R.id.statusText)
        
        // Initialize redirection text labels
        spotifyRedirectText = findViewById(R.id.spotifyRedirectText)
        youtubeRedirectText = findViewById(R.id.youtubeRedirectText)
        shazamRedirectText = findViewById(R.id.shazamRedirectText)
        
        // Initialize test URL components
        testUrlInput = findViewById(R.id.testUrlInput)
        testUrlButton = findViewById(R.id.testUrlButton)
        
        // Setup settings button
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Setup refresh button
        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            updateStatusIndicators()
        }
        
        // Setup test URL button
        testUrlButton.setOnClickListener {
            processTestUrl()
        }
        
        // Show first-run dialog if needed
        if (preferencesHelper.isFirstRun()) {
            showFirstRunDialog()
        }
        
        // Check if app was opened with a link intent
        handleIncomingIntent(intent)
        
        // Update status indicators
        updateStatusIndicators()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }
    
    private fun handleIncomingIntent(intent: Intent) {
        // Check if app was opened with a link
        if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            if (uri != null) {
                // We received a URL directly - forward to RedirectActivity
                val redirectIntent = Intent(this, RedirectActivity::class.java)
                redirectIntent.action = Intent.ACTION_VIEW
                redirectIntent.data = uri
                startActivity(redirectIntent)
            }
        }
    }
    
    private fun processTestUrl() {
        val url = testUrlInput.text.toString().trim()
        if (url.isNotEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
            // Forward to RedirectActivity for all music URLs
            val intent = Intent(this, RedirectActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            startActivity(intent)
        } else {
            AlertDialog.Builder(this)
                .setTitle("Invalid URL")
                .setMessage("Please enter a valid URL starting with http:// or https://")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Update indicators when returning to the app
        updateStatusIndicators()
    }
    
    private fun updateStatusIndicators() {
        // Update all redirections indicator
        val allEnabled = preferencesHelper.isAllRedirectionsEnabled()
        allRedirectionsIndicator.setBackgroundResource(
            if (allEnabled) R.drawable.status_green else R.drawable.status_red
        )
        
        // Update preferred platform text
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        val preferredPlatformName = if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            getString(R.string.platform_spotify)
        } else {
            getString(R.string.platform_youtube_music)
        }
        preferredPlatformText.text = preferredPlatformName
        
        // Update individual platform indicators and texts
        updateSpotifyStatus(preferredPlatform)
        updateYouTubeMusicStatus(preferredPlatform)
        updateShazamStatus(preferredPlatform)
        
        // Update overall status
        updateOverallStatus()
    }
    
    private fun updateSpotifyStatus(preferredPlatform: String) {
        val shouldRedirect = preferencesHelper.shouldRedirectSpotify()
        spotifyRedirectionIndicator.setBackgroundResource(
            if (shouldRedirect) R.drawable.status_green else R.drawable.status_red
        )
        
        if (shouldRedirect) {
            val targetName = if (preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                getString(R.string.platform_youtube_music)
            } else {
                "Not redirected"
            }
            spotifyRedirectText.text = targetName
        } else {
            spotifyRedirectText.text = "Disabled"
        }
    }
    
    private fun updateYouTubeMusicStatus(preferredPlatform: String) {
        val shouldRedirect = preferencesHelper.shouldRedirectYouTubeMusic()
        youtubeRedirectionIndicator.setBackgroundResource(
            if (shouldRedirect) R.drawable.status_green else R.drawable.status_red
        )
        
        if (shouldRedirect) {
            val targetName = if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                getString(R.string.platform_spotify)
            } else {
                "Not redirected"
            }
            youtubeRedirectText.text = targetName
        } else {
            youtubeRedirectText.text = "Disabled"
        }
    }
    
    private fun updateShazamStatus(preferredPlatform: String) {
        val shouldRedirect = preferencesHelper.shouldRedirectShazam()
        shazamRedirectionIndicator.setBackgroundResource(
            if (shouldRedirect) R.drawable.status_green else R.drawable.status_red
        )
        
        if (shouldRedirect) {
            val targetName = if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                getString(R.string.platform_spotify)
            } else if (preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                getString(R.string.platform_youtube_music)
            } else {
                "Not redirected"
            }
            shazamRedirectText.text = targetName
        } else {
            shazamRedirectText.text = "Disabled"
        }
    }
    
    private fun updateOverallStatus() {
        val isConfigured = preferencesHelper.isAllRedirectionsEnabled() && 
                           (preferencesHelper.shouldRedirectSpotify() || 
                            preferencesHelper.shouldRedirectYouTubeMusic() || 
                            preferencesHelper.shouldRedirectShazam())
        
        if (isConfigured) {
            statusText.text = "Ready"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            statusText.text = "Not Ready"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }
    
    private fun showFirstRunDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_first_run, null)
        val dialogButton = dialogView.findViewById<Button>(R.id.dialogButton)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        dialogButton.setOnClickListener {
            preferencesHelper.setFirstRunComplete()
            dialog.dismiss()
        }
        
        dialog.show()
    }
} 