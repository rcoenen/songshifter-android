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
import com.google.android.material.textfield.TextInputEditText
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.RadioGroup
import android.widget.RadioButton

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
    
    // Test URL components
    private lateinit var testUrlInput: TextInputEditText
    private lateinit var testUrlButton: Button
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
            val customView = layoutInflater.inflate(R.layout.action_bar, null)
            actionBar.customView = customView
        }
        
        preferencesHelper = PreferencesHelper(this)
        
        // Initialize views
        spotifyRedirectionIndicator = findViewById(R.id.spotifyRedirectionIndicator)
        youtubeRedirectionIndicator = findViewById(R.id.youtubeRedirectionIndicator)
        shazamRedirectionIndicator = findViewById(R.id.shazamRedirectionIndicator)
        platformRadioGroup = findViewById(R.id.platformRadioGroup)
        warningBanner = findViewById(R.id.warningBanner)
        statusBanner = findViewById(R.id.statusBanner)
        
        // Initialize redirection text labels
        spotifyRedirectText = findViewById(R.id.spotifyRedirectText)
        youtubeRedirectText = findViewById(R.id.youtubeRedirectText)
        shazamRedirectText = findViewById(R.id.shazamRedirectText)
        
        // Initialize layouts
        youtubeRedirectLayout = findViewById(R.id.youtubeRedirectLayout)
        spotifyRedirectLayout = findViewById(R.id.spotifyRedirectLayout)
        
        // Initialize test URL components
        testUrlInput = findViewById(R.id.testUrlInput)
        testUrlButton = findViewById(R.id.testUrlButton)
        buildVersionTextView = findViewById(R.id.buildVersionTextView)
        
        // Initialize test URL with the appropriate example based on platform
        updateTestUrl(preferencesHelper.getPreferredPlatform())
        
        // Set build version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val buildText = "BUILD: ${packageInfo.versionCode} (${packageInfo.versionName})"
            buildVersionTextView.text = buildText
            android.util.Log.d("MusicRedirector", "Setting build version to: $buildText")
            Toast.makeText(this, "App Version: $buildText", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            buildVersionTextView.text = "BUILD: Unknown"
            android.util.Log.e("MusicRedirector", "Error getting package info", e)
        }
        
        // Setup platform toggle group
        setupPlatformToggleGroup()
        
        // Set up click listeners for the banners
        statusBanner.setOnClickListener {
            openAppLinkSettings()
        }
        
        warningBanner.setOnClickListener {
            openAppLinkSettings()
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
            
            // Also show link setup instructions
            showLinkAssociationDialog()
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
        setupPlatformSelection() // Re-add the listener
        
        // Update test URL
        updateTestUrl(preferredPlatform)
        
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
        
        // Update banner visibility
        if (isLinkInterceptionActive) {
            statusBanner.visibility = View.VISIBLE
            warningBanner.visibility = View.GONE
        } else {
            statusBanner.visibility = View.GONE
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
        // Check if our app is set up to handle Spotify links
        val isLinkInterceptionActive = isAppLinksEnabled("open.spotify.com")
        
        // Update UI based on interception status
        if (isLinkInterceptionActive) {
            // Link interception is active - show status banner
            warningBanner.visibility = View.GONE
            statusBanner.visibility = View.VISIBLE
            
            // Set click listener for status banner
            statusBanner.setOnClickListener {
                openAppLinkSettings()
            }
        } else {
            // Link interception is not active - show warning banner
            warningBanner.visibility = View.VISIBLE
            statusBanner.visibility = View.GONE
            
            // Set click listener for warning banner
            warningBanner.setOnClickListener {
                openAppLinkSettings()
            }
        }
        
        // Make sure test URL is populated based on current platform
        updateTestUrl(preferencesHelper.getPreferredPlatform())
    }
    
    private fun isAppLinksEnabled(domain: String): Boolean {
        try {
            // Check if we can directly get verified domains
            val pm = packageManager
            
            // First approach: Try to check if our app is listed as a direct handler
            // This is more permissive, as we only need to confirm our app shows up at all
            val testUri = Uri.parse("https://$domain/test")
            val testIntent = Intent(Intent.ACTION_VIEW, testUri)
            val resolveInfoList = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Return true if we're in the list at all, since the user has enabled us in settings
            val isInList = resolveInfoList.any { resolveInfo ->
                resolveInfo.activityInfo.packageName == packageName
            }
            
            if (isInList) {
                return true
            }
            
            // Second approach: Just check if we have permission to open the links
            // This works on some devices where the first approach fails
            return pm.getComponentEnabledSetting(
                ComponentName(packageName, RedirectActivity::class.java.name)
            ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: assume enabled if there's an error
            return true 
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
    
    private fun setupPlatformToggleGroup() {
        // Listen for changes
        platformRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val newPlatform = when (checkedId) {
                    R.id.radio_spotify -> PreferencesHelper.PLATFORM_SPOTIFY
                    R.id.radio_youtube_music -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                    else -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                }
                
                // Update preference
                preferencesHelper.setPreferredPlatform(newPlatform)
                
                // Update test URL based on selected platform
                updateTestUrl(newPlatform)
                
                // Update the UI
                updateStatusIndicators()
            }
        }
    }
    
    private fun setupPlatformSelection() {
        // Listen for changes
        platformRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newPlatform = when (checkedId) {
                R.id.radio_spotify -> PreferencesHelper.PLATFORM_SPOTIFY
                R.id.radio_youtube_music -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                else -> PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
            }
            
            // Update preference
            preferencesHelper.setPreferredPlatform(newPlatform)
            
            // Update test URL based on selected platform
            updateTestUrl(newPlatform)
            
            // Update the UI
            updateStatusIndicators()
        }
    }
    
    private fun updateTestUrl(platform: String) {
        if (platform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // If Spotify is selected, show YouTube Music test URL
            testUrlInput.setText(EXAMPLE_YOUTUBE_MUSIC_URL)
        } else {
            // If YouTube Music is selected, show Spotify test URL
            testUrlInput.setText(EXAMPLE_SPOTIFY_URL)
        }
    }
} 