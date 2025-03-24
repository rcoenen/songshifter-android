package com.musicredirector

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.RadioGroup
import android.widget.RadioButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.core.content.pm.PackageInfoCompat

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesHelper: PreferencesHelper
    
    // Status indicators
    private lateinit var spotifyRedirectionIndicator: View
    private lateinit var youtubeRedirectionIndicator: View
    private lateinit var shazamRedirectionIndicator: View
    private lateinit var platformRadioGroup: RadioGroup
    private lateinit var warningBanner: TextView
    private lateinit var statusBanner: TextView
    
    // Redirection text labels
    private lateinit var spotifyRedirectText: TextView
    private lateinit var youtubeRedirectText: TextView
    private lateinit var shazamRedirectText: TextView
    
    // Build version
    private lateinit var buildVersionTextView: TextView
    
    // Layouts
    private lateinit var youtubeRedirectLayout: View
    private lateinit var spotifyRedirectLayout: View
    
    // Example URLs for testing
    private val EXAMPLE_SPOTIFY_URL = "https://open.spotify.com/track/6ciGSCeUiA46HANRzcq8o0?si=hNwYN8OsReahI3JecgRaFg"
    private val EXAMPLE_YOUTUBE_MUSIC_URL = "https://music.youtube.com/watch?v=BsoaesiWaCo&si=W2A3SeZGupYgMApr"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up custom action bar
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
            
            // Create custom view for action bar
            val customView = LayoutInflater.from(this).inflate(R.layout.action_bar_custom, null)
            
            // Set up build version in action bar
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val buildVersion = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
                customView.findViewById<TextView>(R.id.buildVersionText).text = "BUILD: $buildVersion"
            } catch (e: Exception) {
                android.util.Log.e("MusicRedirector", "Error getting package info", e)
            }
            
            actionBar.customView = customView
        }
        
        // Initialize views
        warningBanner = findViewById(R.id.warningBanner)
        statusBanner = findViewById(R.id.statusBanner)
        platformRadioGroup = findViewById(R.id.platformRadioGroup)
        youtubeRedirectLayout = findViewById(R.id.youtubeRedirectLayout)
        spotifyRedirectLayout = findViewById(R.id.spotifyRedirectLayout)
        youtubeRedirectionIndicator = findViewById(R.id.youtubeRedirectionIndicator)
        spotifyRedirectionIndicator = findViewById(R.id.spotifyRedirectionIndicator)
        shazamRedirectionIndicator = findViewById(R.id.shazamRedirectionIndicator)
        
        // Initialize preferences helper
        preferencesHelper = PreferencesHelper(this)
        
        // Set up UI
        setupPlatformRadioGroup()
        setupStatusBanner()
        
        // Update initial state
        updateWarningBannerVisibility()
        updateStatusIndicators()
        
        // Set up test button
        setupTestButton()
        
        // Handle any incoming intent
        handleIncomingIntent(intent)
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
        // Remove this unused function since we now use processTestUrl(url: String)
    }
    
    override fun onResume() {
        super.onResume()
        // Update indicators when returning to the app
        updateStatusIndicators()
        
        // Check if link interception is properly configured
        checkLinkInterceptionStatus()
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
        
        // Check if link interception is active
        val isLinkInterceptionActive = isAppLinksEnabled("open.spotify.com")
        
        // Update indicators based on preferred platform
        if (preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
            // When YouTube Music is preferred, show Spotify and hide YouTube Music
            spotifyRedirectLayout.visibility = View.VISIBLE
            spotifyRedirectionIndicator.setBackgroundResource(
                if (preferencesHelper.isSpotifyRedirectionEnabled()) 
                    R.drawable.indicator_active 
                else 
                    R.drawable.indicator_inactive
            )
            
            // Hide YouTube Music redirection (since it's the preferred platform)
            youtubeRedirectLayout.visibility = View.GONE
            
            shazamRedirectionIndicator.setBackgroundResource(
                if (preferencesHelper.isShazamRedirectionEnabled()) 
                    R.drawable.indicator_active 
                else 
                    R.drawable.indicator_inactive
            )
        } else {
            // When Spotify is preferred, hide Spotify and show YouTube Music
            spotifyRedirectLayout.visibility = View.GONE
            
            // Show YouTube Music redirection
            youtubeRedirectLayout.visibility = View.VISIBLE
            youtubeRedirectionIndicator.setBackgroundResource(
                if (preferencesHelper.isYouTubeMusicRedirectionEnabled()) 
                    R.drawable.indicator_active 
                else 
                    R.drawable.indicator_inactive
            )
            
            shazamRedirectionIndicator.setBackgroundResource(
                if (preferencesHelper.isShazamRedirectionEnabled()) 
                    R.drawable.indicator_active 
                else 
                    R.drawable.indicator_inactive
            )
        }
        
        // Update status banner visibility only
        if (isLinkInterceptionActive) {
            statusBanner.visibility = View.VISIBLE
        } else {
            statusBanner.visibility = View.GONE
            warningBanner.text = getString(R.string.warning_link_interception)
            warningBanner.visibility = View.VISIBLE
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
    
    private fun showLinkAssociationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Link Interception")
            .setMessage("To intercept links from Spotify, YouTube Music and other apps, you need to set up link associations.\n\nClick 'Open Settings' below to configure which links this app should intercept.")
            .setPositiveButton("Open Settings") { _, _ -> 
                try {
                    // Open app details settings
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.setData(uri)
                    startActivity(intent)
                    
                    // Show follow-up instruction toast
                    Toast.makeText(this, "Tap 'Open by default', then 'Add link'", 
                        Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't open settings. Please configure manually.", 
                        Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }
    
    private fun checkLinkInterceptionStatus() {
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        // Check if our app is set up to handle links for the preferred platform
        val domain = if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            "music.youtube.com" // When Spotify is preferred, we need to intercept YouTube Music links
        } else {
            "open.spotify.com" // When YouTube Music is preferred, we need to intercept Spotify links
        }
        
        val isLinkInterceptionActive = isAppLinksEnabled(domain)
        
        // Update UI based on interception status
        if (isLinkInterceptionActive) {
            // Link interception is active - show status banner
            statusBanner.visibility = View.VISIBLE
            statusBanner.text = "Link interception active! Click to modify\n" +
                              "Currently intercepting: ${if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) "YouTube Music" else "Spotify"} links"
            
            // Set click listener for status banner
            statusBanner.setOnClickListener {
                openAppLinkSettings()
            }
            
            // Hide warning banner since links are properly set up
            warningBanner.visibility = View.GONE
        } else {
            // Link interception is not active - show warning banner
            statusBanner.visibility = View.GONE
            warningBanner.text = "Link interception not active for ${if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) "YouTube Music" else "Spotify"} links.\nTap here to configure."
            warningBanner.visibility = View.VISIBLE
            warningBanner.setOnClickListener {
                openAppLinkSettings()
            }
        }
    }
    
    private fun isAppLinksEnabled(domain: String): Boolean {
        try {
            val pm = packageManager
            
            // Create a test URI for the specific domain we want to check
            val testUri = Uri.parse("https://$domain/test")
            val testIntent = Intent(Intent.ACTION_VIEW, testUri)
            
            // Query for activities that can handle this URI, requiring exact match
            val resolveInfoList = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Check if our app is the default handler for this domain
            val ourPackageName = packageName
            val isDefaultHandler = resolveInfoList.any { resolveInfo ->
                resolveInfo.activityInfo.packageName == ourPackageName &&
                resolveInfo.activityInfo.name == "com.musicredirector.RedirectActivity"
            }
            
            // Only return true if we're actually set up as the handler
            return isDefaultHandler
            
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's an error, assume we're NOT enabled (safer default)
            return false 
        }
    }
    
    private fun openAppLinkSettings() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Link Interception Not Active")
            .setMessage("This app won't be able to intercept Spotify links until you enable link handling in your device settings.\n\nWould you like to set this up now?")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    // Open app details settings
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.setData(uri)
                    startActivity(intent)
                    
                    // Show follow-up instruction toast
                    Toast.makeText(this, "Tap 'Open by default', then 'Add link'", 
                        Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't open settings. Please configure manually.", 
                        Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }
    
    private fun isYouTubeMusicInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.youtube.music", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun isYouTubeMusicHandlingLinks(): Boolean {
        try {
            val pm = packageManager
            val testUri = Uri.parse("https://music.youtube.com/watch?v=test")
            val testIntent = Intent(Intent.ACTION_VIEW, testUri)
            
            // Query for activities that can handle this URI
            val resolveInfoList = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Check if YouTube Music is in the list of handlers
            return resolveInfoList.any { resolveInfo ->
                resolveInfo.activityInfo.packageName == "com.google.android.apps.youtube.music"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun isYouTubeMusicDisabled(): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo("com.google.android.apps.youtube.music", 0)
            !appInfo.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            // If app is not found, consider it effectively disabled
            true
        }
    }
    
    private fun showYouTubeMusicWarningDialog() {
        // First check if YouTube Music is disabled
        if (isYouTubeMusicDisabled()) {
            // YouTube Music is already disabled, no need to show warning
            return
        }
        
        // Then check if it's handling links
        if (!isYouTubeMusicHandlingLinks()) {
            // YouTube Music link handling is already disabled, no need to show warning
            return
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Action Required")
            .setMessage("Since you've selected Spotify as your preferred platform, you need to disable YouTube Music's link handling to ensure links open in Spotify.\n\nWould you like to open YouTube Music's settings now?")
            .setPositiveButton("Open Settings") { _, _ ->
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
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }
    
    private fun setupPlatformRadioGroup() {
        platformRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newPlatform = when (checkedId) {
                R.id.radio_youtube_music -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                R.id.radio_spotify -> PreferencesHelper.PLATFORM_SPOTIFY
                else -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
            }
            
            // If switching to Spotify and YouTube Music is installed, enabled, and handling links, show warning
            if (newPlatform == PreferencesHelper.PLATFORM_SPOTIFY && 
                isYouTubeMusicInstalled() && 
                !isYouTubeMusicDisabled() &&
                isYouTubeMusicHandlingLinks()) {
                showYouTubeMusicWarningDialog()
            }
            
            preferencesHelper.setPreferredPlatform(newPlatform)
            updateWarningBannerVisibility()
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
    
    private fun updateTestUrl(platform: String) {
        if (platform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // If Spotify is selected, show YouTube Music test URL
            findViewById<TextInputEditText>(R.id.testUrlInput).setText(EXAMPLE_YOUTUBE_MUSIC_URL)
        } else {
            // If YouTube Music is selected, show Spotify test URL
            findViewById<TextInputEditText>(R.id.testUrlInput).setText(EXAMPLE_SPOTIFY_URL)
        }
    }
    
    private fun showTestDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.dialog_test_url)
            .show()
        
        val testUrlInput = dialog.findViewById<TextInputEditText>(R.id.testUrlInput)
        val testUrlButton = dialog.findViewById<MaterialButton>(R.id.testUrlButton)
        
        // Set initial test URL based on current platform
        val platform = preferencesHelper.getPreferredPlatform()
        testUrlInput?.setText(
            if (platform == PreferencesHelper.PLATFORM_SPOTIFY) {
                EXAMPLE_YOUTUBE_MUSIC_URL
            } else {
                EXAMPLE_SPOTIFY_URL
            }
        )
        
        testUrlButton?.setOnClickListener {
            val url = testUrlInput?.text?.toString()
            if (!url.isNullOrBlank()) {
                processTestUrl(url)
                dialog.dismiss()
            }
        }
    }
    
    private fun processTestUrl(url: String) {
        lifecycleScope.launch {
            try {
                val songInfo = MusicLinkExtractor.extractSongInfo(this@MainActivity, url)
                if (songInfo != null) {
                    val searchUrl = MusicLinkExtractor.buildSearchUrl(songInfo, preferencesHelper.getPreferredPlatform())
                    openUrl(searchUrl)
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.error_extracting_info), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.error_processing_url), Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showBuildVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val buildVersion = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
            val buildText = "BUILD: $buildVersion"
            buildVersionTextView.text = buildText
            android.util.Log.d("MusicRedirector", "Setting build version to: $buildText")
        } catch (e: Exception) {
            buildVersionTextView.text = "BUILD: Unknown"
            android.util.Log.e("MusicRedirector", "Error getting package info", e)
        }
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_opening_url), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateWarningBannerVisibility() {
        if (preferencesHelper.getPreferredPlatform() == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
            warningBanner.text = getString(R.string.warning_youtube_music_background)
            warningBanner.visibility = View.VISIBLE
            warningBanner.setOnClickListener(null)  // Remove click listener for this warning
        } else {
            warningBanner.visibility = View.GONE
        }
    }
    
    private fun setupTestButton() {
        findViewById<MaterialButton>(R.id.testButton).setOnClickListener {
            showTestDialog()
        }
    }
    
    private fun setupStatusBanner() {
        // Implementation of setupStatusBanner method
    }
} 