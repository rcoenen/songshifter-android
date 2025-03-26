package com.songshifter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.songshifter.platform.YouTubeMusicStatusFactory

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
    private val ytMusicStatusFactory = YouTubeMusicStatusFactory(context)
    
    /**
     * Updates all UI status indicators based on current app state
     */
    fun updateStatusIndicators() {
        // First check app status (YouTube Music and Spotify)
        // This needs to be done before checking link interception
        checkAppStatus()
        
        // Then check link interception status only if app requirements are met
        checkLinkInterceptionStatus()
        
        // Check if everything is configured correctly and show the "all good" message if it is
        updateAllGoodStatus()
    }
    
    /**
     * Check if all configurations are correct and show the "all good" message if they are
     */
    private fun updateAllGoodStatus() {
        // Check verification status for both domains
        val ytMusicLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("music.youtube.com")
        val spotifyLinksVerified = linkVerificationManager.isDomainVerifiedInSettings("open.spotify.com")
        
        // Get the app handling status
        val ytMusicHandlingLinks = youTubeMusicHandler.isYouTubeMusicHandlingLinks()
        val spotifyInstalled = spotifyHandler.isSpotifyInstalled()
        
        // Check if everything is correctly configured based on the selected platform
        val allConfigured = if (preferencesHelper.getPreferredPlatform() == PreferencesHelper.PLATFORM_SPOTIFY) {
            (!ytMusicHandlingLinks) && ytMusicLinksVerified
        } else {
            (!spotifyInstalled) && spotifyLinksVerified
        }
        
        // Update header text based on configuration status
        val setupStatusHeader = findViewById<TextView>(R.id.setupStatusHeader)
        setupStatusHeader.text = if (allConfigured) {
            "Setup Status"
        } else {
            "Setup Status: ⚠️ Incomplete"
        }
        
        // Show/hide UI states based on configuration
        val configStatusLayout = findViewById<View>(R.id.configStatusLayout)
        val mainTestButton = findViewById<Button>(R.id.mainTestButton)
        
        configStatusLayout.visibility = View.VISIBLE
        
        // Enable/disable the test button
        mainTestButton.isEnabled = allConfigured
        mainTestButton.text = "TEST REDIRECTION"
    }
    
    /**
     * Checks the status of app installations and settings
     */
    private fun checkAppStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Get all status views
        val ytMusicLayout = findViewById<View>(R.id.ytMusicStatusLayout)
        val ytMusicStatusIcon = findViewById<View>(R.id.ytMusicStatusIcon)
        val ytMusicStatusText = findViewById<TextView>(R.id.ytMusicStatusText)
        val ytMusicSettingsButton = findViewById<Button>(R.id.openYTMusicSettingsButton)
        
        val spotifyLayout = findViewById<View>(R.id.spotifyStatusLayout)
        val spotifyStatusIcon = findViewById<View>(R.id.spotifyStatusIcon)
        val spotifyStatusText = findViewById<TextView>(R.id.spotifyStatusText)
        val spotifySettingsButton = findViewById<Button>(R.id.openSpotifySettingsButton)
        
        // First hide all status rows
        ytMusicLayout.visibility = View.GONE
        spotifyLayout.visibility = View.GONE
        
        // For YouTube Music → Spotify (preferredPlatform == PLATFORM_SPOTIFY)
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // STEP 1: Check if YouTube Music is installed but disabled
            ytMusicLayout.tag = "step1"
            ytMusicLayout.visibility = View.VISIBLE
            
            if (youTubeMusicHandler.isYouTubeMusicInstalled()) {
                ytMusicLayout.visibility = View.VISIBLE
                
                val isYtMusicHandlingLinks = youTubeMusicHandler.isYouTubeMusicHandlingLinks()
                ytMusicStatusIcon.setBackgroundResource(
                    if (!isYtMusicHandlingLinks) R.drawable.status_green else R.drawable.status_red
                )
                
                if (!isYtMusicHandlingLinks) {
                    ytMusicStatusText.text = "1. ✓ YouTube Music app disabled"
                    ytMusicStatusText.setTextColor(context.getColor(android.R.color.tab_indicator_text))
                    ytMusicStatusText.paintFlags = ytMusicStatusText.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                } else {
                    ytMusicStatusText.text = "1. ✗ YouTube Music app needs to be disabled (tap to fix)"
                    ytMusicStatusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                    ytMusicStatusText.paintFlags = ytMusicStatusText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                }
                
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
            
            // STEP 2: Check if Spotify is installed
            spotifyLayout.tag = "step2"
            spotifyLayout.visibility = View.VISIBLE
            
            if (!spotifyHandler.isSpotifyInstalled()) {
                // Spotify needs to be installed in this mode
                spotifyStatusIcon.setBackgroundResource(R.drawable.status_red)
                spotifyStatusText.text = "2. ✗ Spotify app needs to be installed (tap to fix)"
                spotifyStatusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                spotifyStatusText.paintFlags = spotifyStatusText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                
                // Make the entire row clickable
                spotifyLayout.isClickable = true
                spotifyLayout.isFocusable = true
                spotifyLayout.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=com.spotify.music")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to browser if Play Store isn't available
                        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Log.e(TAG, "Failed to open Spotify in Play Store or browser: ${e2.message}")
                        }
                    }
                }
                
                // Hide the dedicated button as we made the row clickable
                spotifySettingsButton.visibility = View.GONE
            } else {
                // Spotify is installed, show success
                spotifyStatusIcon.setBackgroundResource(R.drawable.status_green)
                spotifyStatusText.text = "2. ✓ Spotify app is installed and ready"
                spotifyStatusText.setTextColor(context.getColor(android.R.color.tab_indicator_text))
                spotifyStatusText.paintFlags = spotifyStatusText.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                spotifyLayout.isClickable = false
                spotifyLayout.isFocusable = false
                spotifyLayout.setOnClickListener(null)
                spotifySettingsButton.visibility = View.GONE
            }
        } 
        // For Spotify → YouTube Music (preferredPlatform == YOUTUBE_MUSIC)
        else {
            // Clear both layouts first to reset their state
            if (ytMusicLayout.parent is ViewGroup) {
                val parent = ytMusicLayout.parent as ViewGroup
                parent.removeView(ytMusicLayout)
                parent.removeView(spotifyLayout)
            }
            
            // Accessing the parent ViewGroup that contains both layouts
            val setupStatusLayout = findViewById<ViewGroup>(R.id.configStatusLayout)
            
            // STEP 1: Use platform-specific check for YouTube Music
            ytMusicLayout.tag = "step1"
            setupStatusLayout.addView(ytMusicLayout, 1) // Add at position 1 (after instructions text)
            ytMusicLayout.visibility = View.VISIBLE
            
            val ytMusicStatus = ytMusicStatusFactory.create()
            
            if (ytMusicStatus.isInCorrectState()) {
                // Success state - YouTube Music is in the right state for this device type
                ytMusicStatusIcon.setBackgroundResource(R.drawable.status_green)
                ytMusicStatusText.text = ytMusicStatus.getSuccessMessage()
                ytMusicStatusText.setTextColor(context.getColor(android.R.color.tab_indicator_text))
                ytMusicStatusText.paintFlags = ytMusicStatusText.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                ytMusicLayout.isClickable = false
                ytMusicLayout.isFocusable = false
                ytMusicLayout.setOnClickListener(null)
                ytMusicSettingsButton.visibility = View.GONE
                
                // IMPORTANT: Check if we need to disable this app as a handler for YouTube Music links
                // to prevent circular redirection in this mode
                checkLinkHandlingConfiguration()
            } else {
                // Action needed state - YouTube Music needs attention
                ytMusicStatusIcon.setBackgroundResource(R.drawable.status_red)
                ytMusicStatusText.text = ytMusicStatus.getActionNeededMessage()
                ytMusicStatusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                ytMusicStatusText.paintFlags = ytMusicStatusText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                
                // Make the entire row clickable for fixing
                ytMusicLayout.isClickable = true
                ytMusicLayout.isFocusable = true
                ytMusicLayout.setOnClickListener {
                    ytMusicStatus.executeFixAction(it)
                }
                
                // Hide the dedicated button as we made the row clickable
                ytMusicSettingsButton.visibility = View.GONE
            }
            
            // STEP 2: Check if Spotify is uninstalled (needs to be uninstalled for this mode)
            spotifyLayout.tag = "step2"
            setupStatusLayout.addView(spotifyLayout, 2) // Add at position 2 (after step 1)
            spotifyLayout.visibility = View.VISIBLE
            
            if (spotifyHandler.isSpotifyInstalled()) {
                spotifyStatusIcon.setBackgroundResource(R.drawable.status_red)
                spotifyStatusText.text = "2. ✗ Spotify app needs to be uninstalled (tap to fix)"
                spotifyStatusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                spotifyStatusText.paintFlags = spotifyStatusText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                
                // Add direct click listener to the text as well
                spotifyStatusText.setOnClickListener {
                    Log.d(TAG, "Spotify text clicked - opening uninstall")
                    spotifyHandler.openSpotifyUninstall()
                }
                spotifyStatusText.isClickable = true
                
                // Make the entire row clickable with explicit feedback
                spotifyLayout.isClickable = true
                spotifyLayout.isFocusable = true
                // Use context compat for better touch feedback
                spotifyLayout.foreground = context.getDrawable(android.R.drawable.list_selector_background)
                spotifyLayout.setOnClickListener { 
                    Log.d(TAG, "Spotify row clicked - opening uninstall")
                    // Add vibration feedback
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                        vibratorManager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                    
                    // Add a toast for confirmation
                    Toast.makeText(context, "Opening Spotify uninstall...", Toast.LENGTH_SHORT).show()
                    
                    // Call the handler
                    spotifyHandler.openSpotifyUninstall()
                }
                
                // Hide the dedicated button as we made the row clickable
                spotifySettingsButton.visibility = View.GONE
            } else {
                spotifyStatusIcon.setBackgroundResource(R.drawable.status_green)
                spotifyStatusText.text = "2. ✓ Spotify app is uninstalled"
                spotifyStatusText.setTextColor(context.getColor(android.R.color.tab_indicator_text))
                spotifyStatusText.paintFlags = spotifyStatusText.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                spotifyLayout.isClickable = false
                spotifyLayout.isFocusable = false
                spotifyLayout.foreground = null
                spotifyLayout.setOnClickListener(null)
                spotifyStatusText.isClickable = false
                spotifyStatusText.setOnClickListener(null)
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
        
        // Hide all link status layouts by default
        ytLinkLayout.visibility = View.GONE
        spotifyLinkLayout.visibility = View.GONE
        
        // Update UI based on the selected platform
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // Check if YouTube Music app is handling links
            val isYtMusicHandlingLinks = youTubeMusicHandler.isYouTubeMusicHandlingLinks()
            
            // YouTube → Spotify: Show YouTube Music link status only if YouTube Music is disabled
            ytLinkLayout.tag = "step3"
            ytLinkLayout.visibility = View.VISIBLE
            
            if (isYtMusicHandlingLinks) {
                // First step not complete yet, show third step as unavailable
                ytLinkStatusIcon.setBackgroundResource(R.drawable.status_grey)
                ytLinkStatusText.text = "3. Configure YouTube Music links (complete step 2 first)"
                ytLinkStatusText.setTextColor(context.getColor(android.R.color.darker_gray))
                ytLinkLayout.isClickable = false
                ytLinkLayout.isFocusable = false
                ytLinkLayout.setOnClickListener(null)
            } else {
                // Step 2 complete, show real status of link interception
                ytLinkStatusIcon.setBackgroundResource(
                    if (ytMusicLinksVerified) R.drawable.status_green else R.drawable.status_red
                )
                
                if (ytMusicLinksVerified) {
                    // Success state
                    ytLinkStatusText.text = "3. ✓ YouTube Music links will be intercepted"
                    ytLinkStatusText.setTextColor(context.getColor(android.R.color.tab_indicator_text))
                    ytLinkStatusText.paintFlags = ytLinkStatusText.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                } else {
                    // Needs action - style as hyperlink
                    ytLinkStatusText.text = "3. ✗ Configure YouTube Music links (tap to fix)"
                    ytLinkStatusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                    ytLinkStatusText.paintFlags = ytLinkStatusText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                }
                    
                // Make the row clickable if links need to be configured
                if (!ytMusicLinksVerified) {
                    ytLinkLayout.isClickable = true
                    ytLinkLayout.isFocusable = true
                    ytLinkLayout.setOnClickListener {
                        linkVerificationManager.openAppLinkSettings()
                    }
                } else {
                    ytLinkLayout.isClickable = false
                    ytLinkLayout.isFocusable = false
                    ytLinkLayout.setOnClickListener(null)
                }
            }
        } else {
            // Spotify → YouTube Music: Show Spotify link status only if Spotify is uninstalled
            spotifyLinkLayout.tag = "step3"
            spotifyLinkLayout.visibility = View.VISIBLE
            
            if (spotifyHandler.isSpotifyInstalled()) {
                // Step 2 not complete yet, show third step as unavailable
                spotifyLinkStatusIcon.setBackgroundResource(R.drawable.status_grey)
                spotifyLinkStatusText.text = "3. Configure Spotify links (complete step 2 first)"
                spotifyLinkStatusText.setTextColor(context.getColor(android.R.color.darker_gray))
                spotifyLinkLayout.isClickable = false
                spotifyLinkLayout.isFocusable = false
                spotifyLinkLayout.setOnClickListener(null)
            } else {
                // Step 2 complete, show real status of link interception
                spotifyLinkStatusIcon.setBackgroundResource(
                    if (spotifyLinksVerified) R.drawable.status_green else R.drawable.status_red
                )
                
                if (spotifyLinksVerified) {
                    // Success state
                    spotifyLinkStatusText.text = "3. ✓ Spotify links will be intercepted"
                    spotifyLinkStatusText.setTextColor(context.getColor(android.R.color.tab_indicator_text))
                    spotifyLinkStatusText.paintFlags = spotifyLinkStatusText.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                } else {
                    // Needs action - style as hyperlink
                    spotifyLinkStatusText.text = "3. ✗ Configure Spotify links (tap to fix)"
                    spotifyLinkStatusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                    spotifyLinkStatusText.paintFlags = spotifyLinkStatusText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                }
                
                // Make the row clickable if links need to be configured
                if (!spotifyLinksVerified) {
                    spotifyLinkLayout.isClickable = true
                    spotifyLinkLayout.isFocusable = true
                    spotifyLinkLayout.setOnClickListener {
                        linkVerificationManager.openAppLinkSettings()
                    }
                } else {
                    spotifyLinkLayout.isClickable = false
                    spotifyLinkLayout.isFocusable = false
                    spotifyLinkLayout.setOnClickListener(null)
                }
            }
        }
    }
    
    /**
     * Checks if the link handling settings are correctly configured
     * Shows a warning and offers to fix the settings if they're incorrect
     */
    private fun checkLinkHandlingConfiguration() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Only check in Spotify → YouTube Music mode
        if (preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
            // Let the link verification manager check if our current settings are correct
            val isCorrectlyConfigured = linkVerificationManager.ensureCorrectLinkHandling(preferredPlatform)
            
            if (!isCorrectlyConfigured) {
                Log.d(TAG, "⚠️ Link handling is incorrectly configured for current mode")
                
                // Just call the function to open settings directly
                linkVerificationManager.disableYouTubeMusicHandling()
            }
        }
    }
    
    /**
     * Helper function to find views with proper casting
     */
    private fun <T : View> findViewById(id: Int): T {
        return (context as MainActivity).findViewById(id)
    }
} 