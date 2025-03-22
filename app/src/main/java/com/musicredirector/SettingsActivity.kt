package com.musicredirector

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings)
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var platformPreference: ListPreference
        private lateinit var platformBanner: TextView
        private lateinit var spotifyPreference: androidx.preference.SwitchPreferenceCompat
        private lateinit var youTubeMusicPreference: androidx.preference.SwitchPreferenceCompat
        private lateinit var shazamPreference: androidx.preference.SwitchPreferenceCompat
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            // Get the platform preference
            platformPreference = findPreference(PreferencesHelper.KEY_PREFERRED_PLATFORM)!!
            
            // Get the source platform preferences
            spotifyPreference = findPreference(PreferencesHelper.KEY_ENABLE_SPOTIFY)!!
            youTubeMusicPreference = findPreference(PreferencesHelper.KEY_ENABLE_YOUTUBE_MUSIC)!!
            shazamPreference = findPreference(PreferencesHelper.KEY_ENABLE_SHAZAM)!!
        }
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            // Get the platform banner view
            platformBanner = requireActivity().findViewById(R.id.platformBanner)
            
            // Update the summary and banner with current selection
            val currentPlatform = platformPreference.value
            updatePlatformSummary(currentPlatform)
            updatePlatformBanner(currentPlatform)
            updateRedirectionVisibility(currentPlatform)
            
            // Listen for changes to update both summary and banner
            platformPreference.setOnPreferenceChangeListener { _, newValue ->
                val platform = newValue as String
                updatePlatformSummary(platform)
                updatePlatformBanner(platform)
                updateRedirectionVisibility(platform)
                true
            }
        }
        
        private fun updateRedirectionVisibility(platform: String) {
            // Hide redirection toggle for the selected platform
            // (it doesn't make sense to redirect a platform to itself)
            spotifyPreference.isVisible = platform != PreferencesHelper.PLATFORM_SPOTIFY
            youTubeMusicPreference.isVisible = platform != PreferencesHelper.PLATFORM_YOUTUBE_MUSIC
            
            // Update titles to make more sense
            val preferredPlatformName = when (platform) {
                PreferencesHelper.PLATFORM_SPOTIFY -> getString(R.string.platform_spotify)
                PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> getString(R.string.platform_youtube_music)
                else -> "selected platform"
            }
            
            if (platform != PreferencesHelper.PLATFORM_SPOTIFY) {
                spotifyPreference.title = "Redirect Spotify to $preferredPlatformName"
            }
            
            if (platform != PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
                youTubeMusicPreference.title = "Redirect YouTube Music to $preferredPlatformName"
            }
            
            shazamPreference.title = "Redirect Shazam to $preferredPlatformName"
        }
        
        private fun updatePlatformSummary(platform: String) {
            val platformName = when (platform) {
                PreferencesHelper.PLATFORM_SPOTIFY -> getString(R.string.platform_spotify)
                PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> getString(R.string.platform_youtube_music)
                else -> "Please select a platform"
            }
            
            platformPreference.summary = "Selected: $platformName"
        }
        
        private fun updatePlatformBanner(platform: String) {
            when (platform) {
                PreferencesHelper.PLATFORM_SPOTIFY -> {
                    platformBanner.text = "Current Platform: Spotify"
                    platformBanner.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    platformBanner.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                PreferencesHelper.PLATFORM_YOUTUBE_MUSIC -> {
                    platformBanner.text = "Current Platform: YouTube Music"
                    platformBanner.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                    platformBanner.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                else -> {
                    platformBanner.text = "⚠️ Please select a preferred platform! ⚠️"
                    platformBanner.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
                    platformBanner.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
            }
        }
    }
} 