package com.musicredirector

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView

/**
 * Manages platform selection and status across the app
 */
class PlatformManager(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper,
    private val youTubeMusicHandler: YouTubeMusicHandler,
    private val spotifyHandler: SpotifyHandler,
    private val linkVerificationManager: LinkVerificationManager,
    private val uiHelpers: UIHelpers
) {
    private val TAG = "PlatformManager"
    
    /**
     * Updates all UI status indicators based on current app state
     */
    fun updateStatusIndicators() {
        // Get current platform preference
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Check link interception status
        checkLinkInterceptionStatus()
        
        // Check app status (YouTube Music and Spotify)
        checkAppStatus()
        
        // Check if everything is configured correctly and show the "all good" message if it is
        updateAllGoodStatus()
    }
    
    /**
     * Check if all configurations are correct and show the "all good" message if they are
     */
    private fun updateAllGoodStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Check verification status for both domains
        val ytMusicLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("music.youtube.com")
        val spotifyLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("open.spotify.com")
        
        // Get the app handling status
        val ytMusicHandlingLinks = youTubeMusicHandler.isYouTubeMusicHandlingLinks()
        val spotifyHandlingLinks = spotifyHandler.isSpotifyHandlingLinks()
        
        // Check if everything is correctly configured based on the selected platform
        val allConfigured = if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // YouTube → Spotify: 
            // - YouTube Music links should be verified
            // - YouTube Music should NOT be handling links
            ytMusicLinksVerified && (!ytMusicHandlingLinks || !youTubeMusicHandler.isYouTubeMusicInstalled())
        } else {
            // Spotify → YouTube Music:
            // - Spotify links should be verified
            // - Spotify should not exist or not be handling links
            spotifyLinksVerified && (!spotifyHandlingLinks || !spotifyHandler.isSpotifyInstalled())
        }
        
        // Show/hide UI states based on configuration
        val configStatusLayout = findViewById<View>(R.id.configStatusLayout)
        val warningStatusLayout = findViewById<View>(R.id.warningStatusLayout)
        
        if (allConfigured) {
            // Show success state, hide warning
            configStatusLayout.visibility = View.VISIBLE
            warningStatusLayout.visibility = View.GONE
            
            // Update the test button - Create new button in success state if needed
            val successTestButton = findViewById<Button>(R.id.successTestButton)
            if (successTestButton != null) {
                successTestButton.text = "TRY IT OUT - TEST REDIRECTION"
                successTestButton.isEnabled = true
            }
        } else {
            // Show warning state, hide success
            configStatusLayout.visibility = View.GONE
            warningStatusLayout.visibility = View.VISIBLE
            
            // Update the test button
            val testButton = findViewById<Button>(R.id.testButton)
            testButton.text = "CONFIGURATION INCOMPLETE"
            testButton.isEnabled = false
        }
    }
    
    /**
     * Checks the status of app installations and settings
     */
    private fun checkAppStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Update YouTube Music app status
        val ytMusicLayout = findViewById<View>(R.id.ytMusicStatusLayout)
        val ytMusicStatusIcon = findViewById<View>(R.id.ytMusicStatusIcon)
        val ytMusicStatusText = findViewById<TextView>(R.id.ytMusicStatusText)
        val ytMusicSettingsButton = findViewById<Button>(R.id.openYTMusicSettingsButton)
        
        // Update Spotify app status
        val spotifyLayout = findViewById<View>(R.id.spotifyStatusLayout)
        val spotifyStatusIcon = findViewById<View>(R.id.spotifyStatusIcon)
        val spotifyStatusText = findViewById<TextView>(R.id.spotifyStatusText)
        val spotifySettingsButton = findViewById<Button>(R.id.openSpotifySettingsButton)
        
        // For YouTube → Spotify (preferredPlatform == SPOTIFY)
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // Show YouTube Music status (needs to be disabled)
            if (youTubeMusicHandler.isYouTubeMusicInstalled()) {
                ytMusicLayout.visibility = View.VISIBLE
                
                val isYtMusicHandlingLinks = youTubeMusicHandler.isYouTubeMusicHandlingLinks()
                ytMusicStatusIcon.setBackgroundResource(
                    if (!isYtMusicHandlingLinks) R.drawable.status_green else R.drawable.status_red
                )
                ytMusicStatusText.text = if (!isYtMusicHandlingLinks) 
                    "✓ YouTube Music app link handling disabled" 
                else 
                    "✗ YouTube Music app needs to be disabled (tap to fix)"
                
                // Make the entire row clickable if YouTube Music is handling links
                if (isYtMusicHandlingLinks) {
                    ytMusicLayout.isClickable = true
                    ytMusicLayout.isFocusable = true
                    ytMusicLayout.setOnClickListener {
                        youTubeMusicHandler.openYouTubeMusicSettings()
                    }
                    
                    // Hide the dedicated button as we made the row clickable
                    ytMusicSettingsButton.visibility = View.GONE
                } else {
                    ytMusicLayout.isClickable = false
                    ytMusicLayout.isFocusable = false
                    ytMusicLayout.setOnClickListener(null)
                    ytMusicSettingsButton.visibility = View.GONE
                }
            } else {
                ytMusicLayout.visibility = View.GONE
                ytMusicSettingsButton.visibility = View.GONE
            }
            
            // Hide Spotify status (not important in this mode)
            spotifyLayout.visibility = View.GONE
            spotifySettingsButton.visibility = View.GONE
        } 
        // For Spotify → YouTube Music (preferredPlatform == YOUTUBE_MUSIC)
        else {
            // Hide YouTube Music status (not important in this mode)
            ytMusicLayout.visibility = View.GONE
            ytMusicSettingsButton.visibility = View.GONE
            
            // Show Spotify status if installed
            if (spotifyHandler.isSpotifyInstalled()) {
                spotifyLayout.visibility = View.VISIBLE
                
                val isSpotifyHandlingLinks = spotifyHandler.isSpotifyHandlingLinks()
                spotifyStatusIcon.setBackgroundResource(
                    if (!isSpotifyHandlingLinks) R.drawable.status_green else R.drawable.status_red
                )
                spotifyStatusText.text = if (!isSpotifyHandlingLinks) 
                    "✓ Spotify app link handling disabled" 
                else 
                    "✗ Spotify app needs to be disabled (tap to fix)"
                
                // Make the entire row clickable if Spotify is handling links
                if (isSpotifyHandlingLinks) {
                    spotifyLayout.isClickable = true
                    spotifyLayout.isFocusable = true
                    spotifyLayout.setOnClickListener {
                        spotifyHandler.openSpotifySettings()
                    }
                    
                    // Hide the dedicated button as we made the row clickable
                    spotifySettingsButton.visibility = View.GONE
                } else {
                    spotifyLayout.isClickable = false
                    spotifyLayout.isFocusable = false
                    spotifyLayout.setOnClickListener(null)
                    spotifySettingsButton.visibility = View.GONE
                }
            } else {
                spotifyLayout.visibility = View.GONE
                spotifySettingsButton.visibility = View.GONE
            }
        }
    }
    
    /**
     * Checks the status of link interception and updates UI accordingly
     */
    private fun checkLinkInterceptionStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Verify domain statuses
        val ytMusicLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("music.youtube.com")
        val spotifyLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("open.spotify.com")
        
        // Log the actual statuses
        Log.d(TAG, "⚡ LINK STATUS: music.youtube.com links are ${if (ytMusicLinksVerified) "VERIFIED" else "NOT VERIFIED"}")
        Log.d(TAG, "⚡ LINK STATUS: open.spotify.com links are ${if (spotifyLinksVerified) "VERIFIED" else "NOT VERIFIED"}")
        
        // Find status indicator views
        val ytLinkLayout = findViewById<View>(R.id.linkInterceptionLayout)
        val ytLinkStatusIcon = findViewById<View>(R.id.linkInterceptionStatusIcon)
        val ytLinkStatusText = findViewById<TextView>(R.id.linkInterceptionStatusText)
        
        val spotifyLinkLayout = findViewById<View>(R.id.spotifyLinkInterceptionLayout)
        val spotifyLinkStatusIcon = findViewById<View>(R.id.spotifyLinkInterceptionStatusIcon)
        val spotifyLinkStatusText = findViewById<TextView>(R.id.spotifyLinkInterceptionStatusText)
        
        // Set overall setup description based on preference
        val setupDescription = findViewById<TextView>(R.id.setupStatusDescription)
        setupDescription.visibility = View.GONE
        
        // Update UI based on the selected platform
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // YouTube → Spotify: Show YouTube Music link status (we need to intercept these)
            ytLinkLayout.visibility = View.VISIBLE
            ytLinkStatusIcon.setBackgroundResource(
                if (ytMusicLinksVerified) R.drawable.status_green else R.drawable.status_red
            )
            ytLinkStatusText.text = if (ytMusicLinksVerified) 
                "✓ YouTube Music links will be intercepted" 
            else
                "✗ Configure YouTube Music links in app settings"
                
            // Hide Spotify link status (not relevant)
            spotifyLinkLayout.visibility = View.GONE
        } else {
            // Spotify → YouTube Music: Show Spotify link status (we need to intercept these)
            spotifyLinkLayout.visibility = View.VISIBLE
            spotifyLinkStatusIcon.setBackgroundResource(
                if (spotifyLinksVerified) R.drawable.status_green else R.drawable.status_red
            )
            spotifyLinkStatusText.text = if (spotifyLinksVerified) 
                "✓ Spotify links will be intercepted" 
            else
                "✗ Configure Spotify links in app settings"
                
            // Hide YouTube Music link status (not relevant)
            ytLinkLayout.visibility = View.GONE
        }
        
        // Setup App Settings button
        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)
        openSettingsButton.setOnClickListener {
            linkVerificationManager.openAppLinkSettings()
        }
    }
    
    /**
     * Helper function to find views with proper casting
     */
    private fun <T : View> findViewById(id: Int): T {
        return (context as MainActivity).findViewById(id)
    }
} 