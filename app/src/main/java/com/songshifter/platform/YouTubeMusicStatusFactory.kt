package com.songshifter.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

/**
 * Factory that provides the appropriate YouTube Music status implementation
 * based on the device type (Pixel vs. Regular Android)
 */
class YouTubeMusicStatusFactory(private val context: Context) {
    
    private val TAG = "YTMusicStatusFactory"
    private val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    
    /**
     * Creates the appropriate YouTube Music status implementation based on device type
     * @return The appropriate implementation - SystemYouTubeMusicStatus for Pixel, RegularYouTubeMusicStatus for others
     */
    fun create(): YouTubeMusicStatus {
        return if (isSystemApp()) {
            Log.d(TAG, "Detected YouTube Music as a system app, using SystemYouTubeMusicStatus")
            SystemYouTubeMusicStatus(context)
        } else {
            Log.d(TAG, "Detected YouTube Music as a regular app, using RegularYouTubeMusicStatus")
            RegularYouTubeMusicStatus(context)
        }
    }
    
    /**
     * Detects if YouTube Music is installed as a system app (Pixel devices)
     * @return true if YouTube Music is a system app, false if it's regular or not installed
     */
    private fun isSystemApp(): Boolean {
        try {
            val appInfo = context.packageManager.getApplicationInfo(YOUTUBE_MUSIC_PACKAGE, 0)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            Log.d(TAG, "YouTube Music system app check: $isSystem")
            return isSystem
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "YouTube Music not found, assuming regular app")
            return false
        }
    }
} 