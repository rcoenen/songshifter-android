package com.songshifter

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Helper class to manage app preferences
 */
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
        
        private const val PREF_NAME = "MusicRedirectorPrefs"
        private const val KEY_TARGET_PLATFORM = "target_platform"
        const val TARGET_YOUTUBE_MUSIC = "YouTube Music"
        const val TARGET_SPOTIFY = "Spotify"
        
        private const val KEY_SPOTIFY_REDIRECTION = "spotify_redirection"
        private const val KEY_YOUTUBE_MUSIC_REDIRECTION = "youtube_music_redirection"
        private const val KEY_SHAZAM_REDIRECTION = "shazam_redirection"
        private const val KEY_YOUTUBE_MUSIC_WARNING_DISMISSED = "youtube_music_warning_dismissed"
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
    
    /**
     * Gets the user's preferred music platform
     */
    fun getPreferredPlatform(): String {
        return prefs.getString(KEY_PREFERRED_PLATFORM, PLATFORM_YOUTUBE_MUSIC)
            ?: PLATFORM_YOUTUBE_MUSIC
    }
    
    /**
     * Sets the user's preferred music platform
     */
    fun setPreferredPlatform(platform: String) {
        prefs.edit()
            .putString(KEY_PREFERRED_PLATFORM, platform)
            .apply()
    }
    
    fun isSpotifyRedirectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_SPOTIFY, true)
    }
    
    fun isYouTubeMusicRedirectionEnabled(): Boolean {
        return if (getPreferredPlatform() == PLATFORM_SPOTIFY) {
            prefs.getBoolean(KEY_ENABLE_YOUTUBE_MUSIC, false)
        } else {
            prefs.getBoolean(KEY_ENABLE_YOUTUBE_MUSIC, true)
        }
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
    
    fun getTargetPlatform(): String {
        return prefs.getString(KEY_TARGET_PLATFORM, TARGET_YOUTUBE_MUSIC) ?: TARGET_YOUTUBE_MUSIC
    }
    
    fun setTargetPlatform(platform: String) {
        prefs.edit().putString(KEY_TARGET_PLATFORM, platform).apply()
    }
    
    fun getYouTubeMusicWarningDismissed(): Boolean {
        return prefs.getBoolean(KEY_YOUTUBE_MUSIC_WARNING_DISMISSED, false)
    }
    
    fun setYouTubeMusicWarningDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_YOUTUBE_MUSIC_WARNING_DISMISSED, dismissed).apply()
    }
} 