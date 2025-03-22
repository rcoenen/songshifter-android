package com.musicredirector

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    companion object {
        private const val KEY_FIRST_RUN = "first_run"
        const val KEY_ENABLE_ALL = "enable_all"
        const val KEY_PREFERRED_PLATFORM = "preferred_platform"
        const val KEY_ENABLE_SPOTIFY = "enable_spotify"
        const val KEY_ENABLE_YOUTUBE_MUSIC = "enable_youtube_music"
        const val KEY_ENABLE_SHAZAM = "enable_shazam"
        
        const val PLATFORM_SPOTIFY = "spotify"
        const val PLATFORM_YOUTUBE_MUSIC = "youtube_music"
    }
    
    fun isFirstRun(): Boolean {
        return prefs.getBoolean(KEY_FIRST_RUN, true)
    }
    
    fun setFirstRunComplete() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }
    
    fun isAllRedirectionsEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_ALL, true)
    }
    
    fun getPreferredPlatform(): String {
        return prefs.getString(KEY_PREFERRED_PLATFORM, PLATFORM_SPOTIFY) ?: PLATFORM_SPOTIFY
    }
    
    fun isSpotifyRedirectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_SPOTIFY, true)
    }
    
    fun isYouTubeMusicRedirectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_YOUTUBE_MUSIC, true)
    }
    
    fun isShazamRedirectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_SHAZAM, true)
    }
    
    fun shouldRedirectSpotify(): Boolean {
        return isAllRedirectionsEnabled() && isSpotifyRedirectionEnabled() && 
                getPreferredPlatform() != PLATFORM_SPOTIFY
    }
    
    fun shouldRedirectYouTubeMusic(): Boolean {
        return isAllRedirectionsEnabled() && isYouTubeMusicRedirectionEnabled() && 
                getPreferredPlatform() != PLATFORM_YOUTUBE_MUSIC
    }
    
    fun shouldRedirectShazam(): Boolean {
        return isAllRedirectionsEnabled() && isShazamRedirectionEnabled()
    }
} 