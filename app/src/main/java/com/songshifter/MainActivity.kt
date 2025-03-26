package com.songshifter

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Main activity for the SongShifter app
 */
class MainActivity : AppCompatActivity() {
    private lateinit var directionSpinner: Spinner
    private lateinit var directionDescription: TextView
    private lateinit var preferencesHelper: PreferencesHelper
    
    // Components
    private lateinit var youTubeMusicHandler: YouTubeMusicHandler
    private lateinit var spotifyHandler: SpotifyHandler
    private lateinit var linkVerificationManager: LinkVerificationManager
    private lateinit var uiHelpers: UIHelpers
    private lateinit var platformManager: PlatformManager
    private lateinit var redirectionHandler: RedirectionHandler
    
    companion object {
        private const val TAG = "MainActivity"
        const val DIRECTION_YOUTUBE_TO_SPOTIFY = 0
        const val DIRECTION_SPOTIFY_TO_YOUTUBE = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize components
        initializeComponents()
        
        // Initialize views
        directionSpinner = findViewById(R.id.directionSpinner)
        directionDescription = findViewById(R.id.directionDescription)
        
        // Set build version
        setBuildVersion()
        
        // Setup direction spinner
        setupDirectionSpinner()
        
        // Setup test button
        setupTestButton()
        
        // Update status indicators for the preferred platform
        platformManager.updateStatusIndicators()
        
        // Handle any incoming intent
        handleIncomingIntent(intent)
    }
    
    private fun initializeComponents() {
        preferencesHelper = PreferencesHelper(this)
        youTubeMusicHandler = YouTubeMusicHandler(this)
        spotifyHandler = SpotifyHandler(this)
        linkVerificationManager = LinkVerificationManager(this)
        uiHelpers = UIHelpers(this, youTubeMusicHandler, spotifyHandler)
        redirectionHandler = RedirectionHandler(this, preferencesHelper)
        platformManager = PlatformManager(
            this,
            preferencesHelper,
            youTubeMusicHandler,
            spotifyHandler,
            linkVerificationManager,
            uiHelpers
        )
    }
    
    private fun setupDirectionSpinner() {
        // Create adapter with direction options
        val directions = arrayOf("YouTube Music → Spotify", "Spotify → YouTube Music")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, directions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        directionSpinner.adapter = adapter
        
        // Set initial selection
        val currentPlatform = preferencesHelper.getPreferredPlatform()
        if (currentPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            directionSpinner.setSelection(DIRECTION_YOUTUBE_TO_SPOTIFY) // Default: YouTube to Spotify
        } else {
            directionSpinner.setSelection(DIRECTION_SPOTIFY_TO_YOUTUBE)
        }
        
        // Update description based on initial selection
        updateDirectionDescription(directionSpinner.selectedItemPosition)
        
        // Set listener for selection changes
        directionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newPlatform = if (position == DIRECTION_YOUTUBE_TO_SPOTIFY) {
                    PreferencesHelper.PLATFORM_SPOTIFY
                } else {
                    PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
                }
                
                // Only update if there's an actual change
                if (newPlatform != preferencesHelper.getPreferredPlatform()) {
                    // Update the preferred platform
                    preferencesHelper.setPreferredPlatform(newPlatform)
                    
                    // Update description
                    updateDirectionDescription(position)
                    
                    // Update status indicators
                    platformManager.updateStatusIndicators()
                    
                    // Check if we need to adjust link handling for the new mode
                    if (newPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                        // Switching to Spotify → YouTube Music mode
                        // We need to check if we're currently handling YouTube Music links
                        // If so, directly disable it to prevent circular redirection
                        val isHandlingCorrect = linkVerificationManager.ensureCorrectLinkHandling(newPlatform)
                        if (!isHandlingCorrect) {
                            linkVerificationManager.disableYouTubeMusicHandling()
                        }
                    }
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun updateDirectionDescription(position: Int) {
        directionDescription.text = if (position == DIRECTION_YOUTUBE_TO_SPOTIFY) {
            "When you select a YouTube Music link, it will open in Spotify"
        } else {
            "When you select a Spotify link, it will open in YouTube Music"
        }
    }
    
    private fun setBuildVersion() {
        val buildVersionText = findViewById<TextView>(R.id.buildVersionText)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            buildVersionText.text = "BUILD: $versionCode"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            buildVersionText.text = "BUILD: Unknown"
        }
    }
    
    private fun setupTestButton() {
        val mainTestButton = findViewById<Button>(R.id.mainTestButton)
        mainTestButton.setOnClickListener {
            redirectionHandler.testPlatformRedirection()
        }
    }
    
    private fun handleIncomingIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            redirectionHandler.handleIncomingUri(uri)
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Update status indicators to reflect any changes made in settings
        platformManager.updateStatusIndicators()
        
        // Log that we've returned to the app
        Log.d(TAG, "App resumed, updating status indicators")
    }
    
    private fun showLinkHandlingConfigurationPrompt() {
        // This method is redundant since we're handling link configuration automatically
        // Directly call the disable method instead
        linkVerificationManager.disableYouTubeMusicHandling()
    }
} 