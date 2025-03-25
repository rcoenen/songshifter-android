package com.songshifter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity that shows detailed instructions on how to set up link handling correctly.
 */
class ConfigurationHelpActivity : AppCompatActivity() {
    
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var instructionsTitle: TextView
    private lateinit var instructionsText: TextView
    private lateinit var openSettingsButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration_help)
        
        preferencesHelper = PreferencesHelper(this)
        
        // Set up views
        instructionsTitle = findViewById(R.id.instructionsTitle)
        instructionsText = findViewById(R.id.instructionsText)
        openSettingsButton = findViewById(R.id.openSettingsButton)
        
        // Set up button listeners
        openSettingsButton.setOnClickListener {
            openAppLinkSettings()
        }
        
        // Populate instructions based on preferred platform
        updateInstructions()
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Link Handling Configuration"
    }
    
    /**
     * Updates the instructions text based on the user's preferred platform
     */
    private fun updateInstructions() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            instructionsTitle.text = "Setup for Spotify as Preferred Platform"
            instructionsText.text = """
                To correctly set up your device to open YouTube Music links in Spotify:
                
                1. This app MUST handle YouTube Music links (music.youtube.com)
                   - This allows us to intercept and redirect these links
                
                2. This app should NOT handle Spotify links (open.spotify.com)
                   - The Spotify app should handle these directly
                
                How to configure:
                
                1. Go to Android Settings > Apps > Music Redirector
                
                2. Tap "Open by default" > "Add link"
                
                3. Make sure:
                   ✓ music.youtube.com links are ENABLED
                   ✗ open.spotify.com links are DISABLED
                
                4. Ensure Spotify app is set to handle open.spotify.com links
                
                This configuration ensures YouTube Music links redirect to Spotify.
            """.trimIndent()
        } else {
            instructionsTitle.text = "Setup for YouTube Music as Preferred Platform"
            instructionsText.text = """
                To correctly set up your device to open Spotify links in YouTube Music:
                
                1. The Spotify app MUST be uninstalled completely
                   - Android prioritizes the official Spotify app for Spotify links
                   - As long as Spotify is installed, it will handle its own links
                   - This is an Android limitation that cannot be bypassed
                
                2. This app MUST handle Spotify links (open.spotify.com)
                   - This allows us to intercept and redirect these links
                
                3. This app should NOT handle YouTube Music links (music.youtube.com)
                   - The YouTube Music app should handle these directly
                
                How to configure:
                
                1. Uninstall Spotify app completely
                
                2. Go to Android Settings > Apps > Music Redirector
                
                3. Tap "Open by default" > "Add link"
                
                4. Make sure:
                   ✓ open.spotify.com links are ENABLED
                   ✗ music.youtube.com links are DISABLED
                
                5. Ensure YouTube Music app is set to handle music.youtube.com links
                
                This configuration ensures Spotify links redirect to YouTube Music.
            """.trimIndent()
        }
    }
    
    /**
     * Opens the Android app link settings for this app
     */
    private fun openAppLinkSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 