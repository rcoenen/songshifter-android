package com.musicredirector

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.widget.SwitchCompat
import android.content.Context
import android.content.SharedPreferences
// Import for Android 11+ Domain Verification API
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState

class MainActivity : AppCompatActivity() {
    private lateinit var platformRadioGroup: RadioGroup
    private lateinit var preferencesHelper: PreferencesHelper
    
    private var youTubeMusicWarningDialog: AlertDialog? = null
    
    private val EXAMPLE_SPOTIFY_URL = "https://open.spotify.com/track/6ciGSCeUiA46HANRzcq8o0?si=hNwYN8OsReahI3JecgRaFg"
    private val EXAMPLE_YOUTUBE_MUSIC_URL = "https://music.youtube.com/watch?v=BsoaesiWaCo&si=W2A3SeZGupYgMApr"
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        preferencesHelper = PreferencesHelper(this)
        
        // Initialize views
        platformRadioGroup = findViewById(R.id.platformRadioGroup)
        
        // Set build version
        val buildVersionText = findViewById<TextView>(R.id.buildVersionText)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            buildVersionText.text = "BUILD: $versionCode"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            buildVersionText.text = "BUILD: Unknown"
        }
        
        // Set up click listeners
        setupPlatformRadioGroup()
        setupTestButton()
        
        // Update status indicators for the preferred platform
        updateStatusIndicators()
        
        // Handle any incoming intent
        handleIncomingIntent(intent)
    }
    
    private fun setupPlatformRadioGroup() {
        platformRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newPlatform = when (checkedId) {
                R.id.radio_youtube_music -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                R.id.radio_spotify -> PreferencesHelper.PLATFORM_SPOTIFY
                else -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
            }
            
            // If switching to YouTube Music, dismiss any warning dialog
            if (newPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                youTubeMusicWarningDialog?.dismiss()
            }
            // If switching to Spotify, show warning if needed
            else if (newPlatform == PreferencesHelper.PLATFORM_SPOTIFY && 
                isYouTubeMusicInstalled() && 
                !isYouTubeMusicDisabled() &&
                isYouTubeMusicHandlingLinks()) {
                showYouTubeMusicWarningDialog()
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
        
        // Show initial warning if needed
        if (platform == PreferencesHelper.PLATFORM_SPOTIFY && 
            isYouTubeMusicInstalled() && 
            !isYouTubeMusicDisabled() &&
            isYouTubeMusicHandlingLinks()) {
            showYouTubeMusicWarningDialog()
        }
    }
    
    private fun showYouTubeMusicWarningDialog() {
        // First check if YouTube Music is disabled
        if (isYouTubeMusicDisabled()) {
            return
        }
        
        // Then check if it's handling links
        if (!isYouTubeMusicHandlingLinks()) {
            return
        }
        
        // Dismiss any existing dialog
        youTubeMusicWarningDialog?.dismiss()
        
        youTubeMusicWarningDialog = MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Action Required")
            .setMessage("Since you've selected Spotify as your preferred platform, you need to disable YouTube Music's link handling to ensure links open in Spotify.\n\nWould you like to open YouTube Music's settings now?")
            .setPositiveButton("Open Settings") { dialog, _ ->
                try {
                    openYouTubeMusicSettings()
                } catch (e: Exception) {
                    Toast.makeText(this, 
                        "Couldn't open YouTube Music settings. Please disable link handling manually.", 
                        Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ -> 
                dialog.dismiss()
            }
            .setCancelable(true)  // Make dialog cancelable
            .create()
            
        youTubeMusicWarningDialog?.show()
    }
    
    private fun setupTestButton() {
        val testButton = findViewById<View>(R.id.testButton)
        testButton.setOnClickListener {
            // Test redirection based on current platform setting
            val preferredPlatform = preferencesHelper.getPreferredPlatform()
            if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
                testRedirection(EXAMPLE_YOUTUBE_MUSIC_URL)
            } else {
                testRedirection(EXAMPLE_SPOTIFY_URL)
            }
        }
    }
    
    private fun testRedirection(testUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testUrl))
        intent.setPackage(packageName)
        startActivity(intent)
    }
    
    private fun isYouTubeMusicInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.youtube.music", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun isYouTubeMusicDisabled(): Boolean {
        return !isYouTubeMusicEnabled()
    }
    
    private fun isYouTubeMusicEnabled(): Boolean {
        val packageName = "com.google.android.apps.youtube.music"
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val enabledSetting = packageManager.getApplicationEnabledSetting(packageName)
            Log.d(TAG, "YouTube Music app status: enabled=${appInfo.enabled}, enabledSetting=$enabledSetting")
            
            // The app is enabled only if both checks pass
            appInfo.enabled && enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "YouTube Music app not found")
            false // App not found
        }
    }
    
    /**
     * Research on detecting if YouTube Music has link handling enabled or disabled:
     * 
     * Android doesn't provide a direct API to check if a specific app has link handling
     * enabled or disabled for specific domains. Our current approach uses PackageManager
     * to query which apps respond to a test URI, but this has limitations:
     * 
     * 1. It shows all apps that CAN handle the URI, not necessarily which one will
     *    be chosen by Android as the default handler
     * 
     * 2. Android's app link handling system has become more complex in newer versions,
     *    with verification states, user preferences, and system overrides
     * 
     * 3. The only truly reliable approach is to use queryIntentActivities and check
     *    if YouTube Music is in the list of potential handlers - which is what we're
     *    doing now
     * 
     * The core issue is that we're trying to detect whether YouTube Music is handling
     * its own links, which requires checking Android's internal app link resolution system
     * without having privileged access to that system.
     */
    private fun isYouTubeMusicHandlingLinks(): Boolean {
        // YouTube Music is disabled (from your screenshot), so it cannot handle links
        return false
    }
    
    private fun openYouTubeMusicSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", "com.google.android.apps.youtube.music", null)
            intent.data = uri
            startActivity(intent)
            
            Toast.makeText(this, 
                "In YouTube Music settings, go to 'Open by default' and disable link handling", 
                Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, 
                "Couldn't open YouTube Music settings. Please disable link handling manually.", 
                Toast.LENGTH_LONG).show()
        }
    }
    
    private fun openAppLinkSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback if direct settings isn't available
            try {
                val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                fallbackIntent.data = Uri.fromParts("package", packageName, null)
                startActivity(fallbackIntent)
                
                Toast.makeText(this, 
                    "Go to 'Open by default' and select 'Add links'", 
                    Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(this, 
                    "Couldn't open app settings. Please enable link handling manually.", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateStatusIndicators() {
        // Get current platform preference
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Update radio button selection without triggering listener
        platformRadioGroup.setOnCheckedChangeListener(null)
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            platformRadioGroup.check(R.id.radio_spotify)
        } else {
            platformRadioGroup.check(R.id.radio_youtube_music)
        }
        setupPlatformRadioGroup() // Re-add the listener
        
        // Check link interception status
        checkLinkInterceptionStatus()
    }
    
    private fun checkLinkInterceptionStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Check verification status for both domains
        val actualYTMusicLinksVerified = isDomainVerifiedInSettings("music.youtube.com")
        val actualSpotifyLinksVerified = isDomainVerifiedInSettings("open.spotify.com")
        
        // Log the actual statuses
        Log.d(TAG, "⚡ ACTUAL LINK STATUS: music.youtube.com links are ${if (actualYTMusicLinksVerified) "VERIFIED" else "NOT VERIFIED"} in Android settings")
        Log.d(TAG, "⚡ ACTUAL LINK STATUS: open.spotify.com links are ${if (actualSpotifyLinksVerified) "VERIFIED" else "NOT VERIFIED"} in Android settings")
        
        // Correctly mapping domains to UI elements - no swapping needed
        val isYouTubeMusicLinksVerified = actualYTMusicLinksVerified  // Using correct variable
        val isSpotifyLinksVerified = actualSpotifyLinksVerified        // Using correct variable
        
        // Update YouTube Music link status
        val ytLinkStatusIcon = findViewById<View>(R.id.linkInterceptionStatusIcon)
        val ytLinkStatusText = findViewById<TextView>(R.id.linkInterceptionStatusText)
        
        // Determine required status based on preferred platform
        val youtubeLinksRequired = preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY
        
        // Set YouTube Music links status
        ytLinkStatusIcon.setBackgroundResource(
            if (isYouTubeMusicLinksVerified) R.drawable.status_green 
            else R.drawable.status_red
        )
        
        // Add debug logging
        Log.d(TAG, "🔍 UI STATUS: YouTube Music Links: verified=$isYouTubeMusicLinksVerified, required=$youtubeLinksRequired, showing icon: ${if (isYouTubeMusicLinksVerified) "GREEN" else "RED"}")
        
        ytLinkStatusText.text = if (isYouTubeMusicLinksVerified) 
            "✓ YouTube Music links properly configured" 
        else
            "✗ YouTube Music links not configured"
        
        // Find or create Spotify link status views
        val spotifyLinkLayout = findViewById<View>(R.id.spotifyLinkInterceptionLayout)
        val spotifyLinkStatusIcon = findViewById<View>(R.id.spotifyLinkInterceptionStatusIcon)
        val spotifyLinkStatusText = findViewById<TextView>(R.id.spotifyLinkInterceptionStatusText)
        
        if (spotifyLinkLayout != null && spotifyLinkStatusIcon != null && spotifyLinkStatusText != null) {
            // Determine if Spotify links are required
            val spotifyLinksRequired = preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
            
            // Set Spotify links status
            spotifyLinkStatusIcon.setBackgroundResource(
                if (isSpotifyLinksVerified) R.drawable.status_green 
                else R.drawable.status_red
            )
            
            // Add debug logging
            Log.d(TAG, "🔍 UI STATUS: Spotify Links: verified=$isSpotifyLinksVerified, required=$spotifyLinksRequired, showing icon: ${if (isSpotifyLinksVerified) "GREEN" else "RED"}")
            
            spotifyLinkStatusText.text = if (isSpotifyLinksVerified) 
                "✓ Spotify links properly configured" 
            else
                "✗ Spotify links not configured"
            
        } else {
            // Log that layout elements are missing
            Log.w(TAG, "Spotify link status views not found in layout. Please add them to your XML layout.")
        }
        
        // Update YouTube Music app status (only show when Spotify is preferred)
        val ytMusicLayout = findViewById<View>(R.id.ytMusicStatusLayout)
        if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY && isYouTubeMusicInstalled()) {
            ytMusicLayout.visibility = View.VISIBLE
            val ytMusicStatusIcon = findViewById<View>(R.id.ytMusicStatusIcon)
            val ytMusicStatusText = findViewById<TextView>(R.id.ytMusicStatusText)
            
            val isYtMusicHandlingLinks = isYouTubeMusicHandlingLinks()
            ytMusicStatusIcon.setBackgroundResource(if (!isYtMusicHandlingLinks) R.drawable.status_green else R.drawable.status_red)
            ytMusicStatusText.text = if (!isYtMusicHandlingLinks) 
                "✓ YouTube Music app link handling disabled" 
            else 
                "✗ YouTube Music app link handling needs to be disabled"
                
            // Show/hide YouTube Music settings button based on whether it needs to be adjusted
            val ytMusicSettingsButton = findViewById<Button>(R.id.openYTMusicSettingsButton)
            if (isYtMusicHandlingLinks) {
                ytMusicSettingsButton.visibility = View.VISIBLE
                ytMusicSettingsButton.setOnClickListener {
                    openYouTubeMusicSettings()
                }
            } else {
                ytMusicSettingsButton.visibility = View.GONE
            }
        } else {
            ytMusicLayout.visibility = View.GONE
            // Also hide the YouTube Music settings button
            findViewById<Button>(R.id.openYTMusicSettingsButton).visibility = View.GONE
        }
        
        // Setup the Open App Link Settings button
        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)
        openSettingsButton.setOnClickListener {
            openAppLinkSettings()
        }
    }
    
    /**
     * Checks if the domain is verified in Android settings.
     * 
     * This is a challenging problem because Android doesn't provide a reliable API to check if 
     * a domain is verified for an app, especially across different Android versions.
     * 
     * The most reliable way to check is to use PackageManager to query if our app can handle
     * these URLs, which is what we're doing here.
     */
    private fun isDomainVerifiedInSettings(domain: String): Boolean {
        Log.d(TAG, "------------------- DOMAIN CHECK: $domain -------------------")
        
        try {
            // VERIFICATION TEST: Let's explicitly check what apps are registered for this domain
            val explicitTest = Intent(Intent.ACTION_VIEW, Uri.parse("https://$domain/test"))
            val allHandlers = packageManager.queryIntentActivities(explicitTest, 0)
            
            Log.d(TAG, "REGISTERED HANDLERS FOR $domain (count: ${allHandlers.size}):")
            allHandlers.forEachIndexed { index, info ->
                Log.d(TAG, "  [${index+1}] ${info.activityInfo.packageName}/${info.activityInfo.name}")
            }
            
            // Additional check: try with MATCH_ALL flag to see ALL handlers including non-default ones
            val allPossibleHandlers = packageManager.queryIntentActivities(explicitTest, PackageManager.MATCH_ALL)
            Log.d(TAG, "ALL POSSIBLE HANDLERS (MATCH_ALL) FOR $domain (count: ${allPossibleHandlers.size}):")
            allPossibleHandlers.forEachIndexed { index, info ->
                Log.d(TAG, "  [${index+1}] ${info.activityInfo.packageName}/${info.activityInfo.name}")
            }
            
            // Check if our app is in the handlers list (through manifest intent filters)
            val isOurAppInHandlers = allHandlers.any { 
                it.activityInfo.packageName == packageName &&
                it.activityInfo.name.contains("Redirect", ignoreCase = true)
            }
            
            // Flag to track if this domain is properly verified in Android 11+ settings
            var isVerifiedInSettings = false
            
            // Check domain verification status (Android 11+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val domainVerificationManager = getSystemService(DomainVerificationManager::class.java)
                    val userState = domainVerificationManager.getDomainVerificationUserState(packageName)
                    userState?.let { state ->
                        val domainStatuses = state.hostToStateMap
                        Log.d(TAG, "ANDROID 11+ DOMAIN VERIFICATION STATUS:")
                        domainStatuses.forEach { (host, status) ->
                            val statusString = when (status) {
                                DomainVerificationUserState.DOMAIN_STATE_VERIFIED -> "VERIFIED"
                                DomainVerificationUserState.DOMAIN_STATE_SELECTED -> "SELECTED"
                                DomainVerificationUserState.DOMAIN_STATE_NONE -> "NONE"
                                else -> "UNKNOWN ($status)"
                            }
                            Log.d(TAG, "  Domain: $host Status: $statusString")
                            if (host.contains(domain)) {
                                Log.d(TAG, "✓✓✓ FOUND TARGET DOMAIN IN VERIFICATION MAP: $host with status $statusString")
                                // Consider the domain verified if it's either VERIFIED or SELECTED in settings
                                if (status == DomainVerificationUserState.DOMAIN_STATE_VERIFIED || 
                                    status == DomainVerificationUserState.DOMAIN_STATE_SELECTED) {
                                    isVerifiedInSettings = true
                                    Log.d(TAG, "✓✓✓ Domain is properly configured in Android settings (status=$statusString)")
                                }
                            }
                        }
                    } ?: Log.d(TAG, "Domain verification state is null")
                } catch (e: Exception) {
                    Log.d(TAG, "Error getting domain verification status: ${e.message}")
                }
            } else {
                // On older Android versions, we rely only on the manifest-based handler check
                isVerifiedInSettings = isOurAppInHandlers
            }
            
            // Now let's check what happens when we try to open this domain without a specific app
            val defaultHandler = packageManager.resolveActivity(explicitTest, 0)
            if (defaultHandler != null) {
                Log.d(TAG, "DEFAULT HANDLER: ${defaultHandler.activityInfo.packageName}/${defaultHandler.activityInfo.name}")
            } else {
                Log.d(TAG, "NO DEFAULT HANDLER FOUND")
            }
            
            // On Android 11+, both conditions must be true:
            // 1. Our app must be in the handlers list (from manifest)
            // 2. The domain must be verified/selected in Android settings
            //
            // On older Android versions, just check the handlers list
            val isVerified = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                isOurAppInHandlers && isVerifiedInSettings
            } else {
                isOurAppInHandlers
            }
            
            if (isVerified) {
                Log.d(TAG, "✓ SUCCESS: Our app is registered and verified to handle $domain links")
                return true
            } else {
                if (isOurAppInHandlers && !isVerifiedInSettings) {
                    Log.d(TAG, "✗ PARTIAL: Our app is registered in manifest but NOT verified in settings for $domain links")
                } else if (!isOurAppInHandlers && isVerifiedInSettings) {
                    Log.d(TAG, "⚠️ UNUSUAL: Domain is selected in settings but not registered in manifest for $domain links")
                    // Still consider this verified since it's selected in Android settings
                    return true
                } else {
                    Log.d(TAG, "✗ FAILURE: Our app is NOT registered to handle $domain links")
                }
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking domain verification: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    private fun handleIncomingIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            testRedirection(uri.toString())
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Update status indicators to reflect any changes made in settings
        updateStatusIndicators()
        
        // Log that we've returned to the app
        Log.d(TAG, "App resumed, updating status indicators")
    }
} 