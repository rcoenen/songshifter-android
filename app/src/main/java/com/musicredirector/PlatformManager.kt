package com.musicredirector

import android.content.Context
import android.widget.RadioGroup
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
    private val linkVerificationManager: LinkVerificationManager,
    private val uiHelpers: UIHelpers
) {
    private val TAG = "PlatformManager"
    
    /**
     * Sets up the platform radio group with appropriate listeners
     */
    fun setupPlatformRadioGroup(platformRadioGroup: RadioGroup) {
        platformRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val resourceId = context.resources.getIdentifier("R.id.radio_youtube_music", null, context.packageName)
            val newPlatform = when (checkedId) {
                R.id.radio_youtube_music -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                R.id.radio_spotify -> PreferencesHelper.PLATFORM_SPOTIFY
                else -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
            }
            
            // If switching to YouTube Music
            if (newPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                uiHelpers.dismissYouTubeMusicWarningDialog()
                
                // Check if YouTube Music is installed but disabled
                if (youTubeMusicHandler.isYouTubeMusicInstalled() && 
                    youTubeMusicHandler.isYouTubeMusicDisabled()) {
                    uiHelpers.showYouTubeMusicDisabledWarningDialog()
                }
            }
            // If switching to Spotify, show warning if needed
            else if (newPlatform == PreferencesHelper.PLATFORM_SPOTIFY && 
                youTubeMusicHandler.isYouTubeMusicInstalled() && 
                !youTubeMusicHandler.isYouTubeMusicDisabled() &&
                youTubeMusicHandler.isYouTubeMusicHandlingLinks()) {
                uiHelpers.showYouTubeMusicWarningDialog()
            }
            
            preferencesHelper.setPreferredPlatform(newPlatform)
            updateStatusIndicators()
        }
        
        // Set initial radio button based on saved preference
        val platform = preferencesHelper.getPreferredPlatform()
        when (platform) {
            PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> platformRadioGroup.check(R.id.radio_youtube_music)
            PreferencesHelper.PLATFORM_SPOTIFY -> platformRadioGroup.check(R.id.radio_spotify)
        }
        
        // Show initial warnings if needed
        if (platform == PreferencesHelper.PLATFORM_SPOTIFY && 
            youTubeMusicHandler.isYouTubeMusicInstalled() && 
            !youTubeMusicHandler.isYouTubeMusicDisabled() &&
            youTubeMusicHandler.isYouTubeMusicHandlingLinks()) {
            uiHelpers.showYouTubeMusicWarningDialog()
        } else if (platform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC &&
            youTubeMusicHandler.isYouTubeMusicInstalled() && 
            youTubeMusicHandler.isYouTubeMusicDisabled()) {
            uiHelpers.showYouTubeMusicDisabledWarningDialog()
        }
    }
    
    /**
     * Updates all UI status indicators based on current app state
     */
    fun updateStatusIndicators() {
        // Get current platform preference
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Check link interception status
        checkLinkInterceptionStatus()
    }
    
    /**
     * Checks the status of link interception and updates UI accordingly
     */
    private fun checkLinkInterceptionStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Check if YouTube Music is selected but disabled
        val youTubeMusicSelected = preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
        val youTubeMusicDisabled = youTubeMusicHandler.isYouTubeMusicInstalled() && 
                                  youTubeMusicHandler.isYouTubeMusicDisabled()
        
        // Show a warning banner if YouTube Music is selected but disabled
        val ytMusicDisabledWarningView = (context as MainActivity).findViewById<View>(R.id.ytMusicDisabledWarningCard)
        if (ytMusicDisabledWarningView != null) {
            if (youTubeMusicSelected && youTubeMusicDisabled) {
                ytMusicDisabledWarningView.visibility = View.VISIBLE
                
                // Add button to open YouTube Music settings
                val enableYTMusicButton = (context as MainActivity).findViewById<Button>(R.id.enableYTMusicButton)
                if (enableYTMusicButton != null) {
                    enableYTMusicButton.setOnClickListener {
                        youTubeMusicHandler.openYouTubeMusicSettings()
                    }
                }
            } else {
                ytMusicDisabledWarningView.visibility = View.GONE
            }
        }
        
        // Check verification status for both domains
        val actualYTMusicLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("music.youtube.com")
        val actualSpotifyLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("open.spotify.com")
        
        // Log the actual statuses
        Log.d(TAG, "⚡ ACTUAL LINK STATUS: music.youtube.com links are ${if (actualYTMusicLinksVerified) "VERIFIED" else "NOT VERIFIED"} in Android settings")
        Log.d(TAG, "⚡ ACTUAL LINK STATUS: open.spotify.com links are ${if (actualSpotifyLinksVerified) "VERIFIED" else "NOT VERIFIED"} in Android settings")
        
        // Check configuration correctness and show appropriate warnings in the UI
        checkConfigurationAndShowWarning(preferredPlatform, actualYTMusicLinksVerified, actualSpotifyLinksVerified)
        
        // Correctly mapping domains to UI elements
        val isYouTubeMusicLinksVerified = actualYTMusicLinksVerified
        val isSpotifyLinksVerified = actualSpotifyLinksVerified
        
        // Find or create Spotify link status views
        val spotifyLinkLayout = (context as MainActivity).findViewById<View>(R.id.spotifyLinkInterceptionLayout)
        val spotifyLinkStatusIcon = (context as MainActivity).findViewById<View>(R.id.spotifyLinkInterceptionStatusIcon)
        val spotifyLinkStatusText = (context as MainActivity).findViewById<TextView>(R.id.spotifyLinkInterceptionStatusText)
        
        // Find YouTube Music link status views
        val ytLinkLayout = (context as MainActivity).findViewById<View>(R.id.linkInterceptionLayout)
        val ytLinkStatusIcon = (context as MainActivity).findViewById<View>(R.id.linkInterceptionStatusIcon)
        val ytLinkStatusText = (context as MainActivity).findViewById<TextView>(R.id.linkInterceptionStatusText)
        
        // Only show relevant indicators based on preferred platform
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // When Spotify is preferred: 
            // - Show YouTube Music link status (we need to intercept these)
            // - Hide Spotify link status (not relevant)
            ytLinkLayout.visibility = View.VISIBLE
            
            // Set YouTube Music links status
            ytLinkStatusIcon.setBackgroundResource(
                if (isYouTubeMusicLinksVerified) R.drawable.status_green 
                else R.drawable.status_red
            )
            
            // Add debug logging
            Log.d(TAG, "🔍 UI STATUS: YouTube Music Links: verified=$isYouTubeMusicLinksVerified, showing icon: ${if (isYouTubeMusicLinksVerified) "GREEN" else "RED"}")
            
            ytLinkStatusText.text = if (isYouTubeMusicLinksVerified) 
                "✓ YouTube Music links configured" 
            else
                "✗ YouTube Music links need to be configured"
                
            // Hide Spotify link layout
            if (spotifyLinkLayout != null) {
                spotifyLinkLayout.visibility = View.GONE
            }
        } else {
            // When YouTube Music is preferred:
            // - Show Spotify link status (we need to intercept these)
            // - Hide YouTube Music link status (not relevant)
            
            if (spotifyLinkLayout != null && spotifyLinkStatusIcon != null && spotifyLinkStatusText != null) {
                spotifyLinkLayout.visibility = View.VISIBLE
                
                // Set Spotify links status
                spotifyLinkStatusIcon.setBackgroundResource(
                    if (isSpotifyLinksVerified) R.drawable.status_green 
                    else R.drawable.status_red
                )
                
                // Add debug logging
                Log.d(TAG, "🔍 UI STATUS: Spotify Links: verified=$isSpotifyLinksVerified, showing icon: ${if (isSpotifyLinksVerified) "GREEN" else "RED"}")
                
                spotifyLinkStatusText.text = if (isSpotifyLinksVerified) 
                    "✓ Spotify links configured" 
                else
                    "✗ Spotify links need to be configured"
            } else {
                // Log that layout elements are missing
                Log.w(TAG, "Spotify link status views not found in layout. Please add them to your XML layout.")
            }
            
            // Hide YouTube Music link layout
            ytLinkLayout.visibility = View.GONE
        }
        
        // Update YouTube Music app status (only show when Spotify is preferred)
        val ytMusicLayout = (context as MainActivity).findViewById<View>(R.id.ytMusicStatusLayout)
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY && youTubeMusicHandler.isYouTubeMusicInstalled()) {
            ytMusicLayout.visibility = View.VISIBLE
            val ytMusicStatusIcon = (context as MainActivity).findViewById<View>(R.id.ytMusicStatusIcon)
            val ytMusicStatusText = (context as MainActivity).findViewById<TextView>(R.id.ytMusicStatusText)
            
            val isYtMusicHandlingLinks = youTubeMusicHandler.isYouTubeMusicHandlingLinks()
            ytMusicStatusIcon.setBackgroundResource(if (!isYtMusicHandlingLinks) R.drawable.status_green else R.drawable.status_red)
            ytMusicStatusText.text = if (!isYtMusicHandlingLinks) 
                "✓ YouTube Music app link handling disabled" 
            else 
                "✗ YouTube Music app link handling needs to be disabled"
                
            // Show/hide YouTube Music settings button based on whether it needs to be adjusted
            val ytMusicSettingsButton = (context as MainActivity).findViewById<Button>(R.id.openYTMusicSettingsButton)
            if (isYtMusicHandlingLinks) {
                ytMusicSettingsButton.visibility = View.VISIBLE
                ytMusicSettingsButton.setOnClickListener {
                    youTubeMusicHandler.openYouTubeMusicSettings()
                }
            } else {
                ytMusicSettingsButton.visibility = View.GONE
            }
        } else {
            ytMusicLayout.visibility = View.GONE
            // Also hide the YouTube Music settings button
            (context as MainActivity).findViewById<Button>(R.id.openYTMusicSettingsButton).visibility = View.GONE
        }
        
        // Setup the Open App Link Settings button
        val openSettingsButton = (context as MainActivity).findViewById<Button>(R.id.openSettingsButton)
        openSettingsButton.setOnClickListener {
            linkVerificationManager.openAppLinkSettings()
        }
    }

    /**
     * Check configuration correctness and show appropriate warnings in the UI
     */
    private fun checkConfigurationAndShowWarning(preferredPlatform: String, actualYTMusicLinksVerified: Boolean, actualSpotifyLinksVerified: Boolean) {
        // Find the warning card and its components
        val configWarningCard = (context as MainActivity).findViewById<View>(R.id.configWarningCard)
        val configWarningTitle = (context as MainActivity).findViewById<TextView>(R.id.configWarningTitle)
        val configWarningText = (context as MainActivity).findViewById<TextView>(R.id.configWarningText)
        val fixConfigButton = (context as MainActivity).findViewById<Button>(R.id.fixConfigButton)
        
        if (configWarningCard == null || configWarningTitle == null || configWarningText == null || fixConfigButton == null) {
            Log.w(TAG, "Configuration warning views not found")
            return
        }
        
        // Determine if configuration is incorrect
        val isConfigIncorrect = if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // For Spotify as preferred platform, we want YouTube Music links verified but not Spotify links
            !actualYTMusicLinksVerified || actualSpotifyLinksVerified
        } else {
            // For YouTube Music as preferred platform, we want Spotify links verified but not YouTube Music links
            !actualSpotifyLinksVerified || actualYTMusicLinksVerified
        }
        
        if (isConfigIncorrect) {
            // Configuration is incorrect, show the warning
            configWarningCard.visibility = View.VISIBLE
            
            if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                configWarningTitle.text = "⚠️ Spotify Configuration Issue"
                configWarningText.text = 
                    "Your preferred platform is Spotify, but your link handling is not configured correctly.\n\n" +
                    "For proper redirection:\n" +
                    "• This app SHOULD handle YouTube Music links\n" +
                    "• This app should NOT handle Spotify links"
            } else {
                configWarningTitle.text = "⚠️ YouTube Music Configuration Issue"
                configWarningText.text = 
                    "Your preferred platform is YouTube Music, but your link handling is not configured correctly.\n\n" +
                    "For proper redirection:\n" +
                    "• This app SHOULD handle Spotify links\n" +
                    "• This app should NOT handle YouTube Music links"
            }
            
            // Set up button to open configuration settings
            fixConfigButton.setOnClickListener {
                linkVerificationManager.openAppLinkSettings()
            }
        } else {
            // Configuration is correct, hide the warning
            configWarningCard.visibility = View.GONE
        }
    }
} 