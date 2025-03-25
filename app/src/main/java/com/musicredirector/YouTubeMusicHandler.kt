package com.musicredirector

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Handler for YouTube Music app specific functionality
 */
class YouTubeMusicHandler(private val context: Context) {
    private val TAG = "YouTubeMusicHandler"
    private val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    
    /**
     * Checks if YouTube Music app is installed on the device
     */
    fun isYouTubeMusicInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(YOUTUBE_MUSIC_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Checks if YouTube Music app is disabled
     */
    fun isYouTubeMusicDisabled(): Boolean {
        return !isYouTubeMusicEnabled()
    }
    
    /**
     * Checks if YouTube Music app is enabled
     */
    private fun isYouTubeMusicEnabled(): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(YOUTUBE_MUSIC_PACKAGE, 0)
            val enabledSetting = context.packageManager.getApplicationEnabledSetting(YOUTUBE_MUSIC_PACKAGE)
            Log.d(TAG, "YouTube Music app status: enabled=${appInfo.enabled}, enabledSetting=$enabledSetting")
            
            // The app is enabled only if both checks pass
            appInfo.enabled && enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "YouTube Music app not found")
            false // App not found
        }
    }
    
    /**
     * Checks if YouTube Music is handling its own links.
     * The app can't handle links if it's disabled, so this returns false in that case.
     * Otherwise, checks if YouTube Music is in the list of handlers for music.youtube.com URLs.
     */
    fun isYouTubeMusicHandlingLinks(): Boolean {
        // YouTube Music can't handle links if it's disabled
        if (isYouTubeMusicDisabled()) {
            return false
        }
        
        // Otherwise, check if it's in the list of handlers
        try {
            val pm = context.packageManager
            val testUri = Uri.parse("https://music.youtube.com/watch?v=test")
            val testIntent = Intent(Intent.ACTION_VIEW, testUri)
            
            // Query for activities that can handle this URI
            val resolveInfoList = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Check if YouTube Music is in the list of handlers
            return resolveInfoList.any { resolveInfo ->
                resolveInfo.activityInfo.packageName == YOUTUBE_MUSIC_PACKAGE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if YouTube Music handles links: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Opens YouTube Music app settings
     */
    fun openYouTubeMusicSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", YOUTUBE_MUSIC_PACKAGE, null)
            intent.data = uri
            context.startActivity(intent)
            
            Toast.makeText(context, 
                "In YouTube Music settings, go to 'Open by default' and disable link handling", 
                Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, 
                "Couldn't open YouTube Music settings. Please disable link handling manually.", 
                Toast.LENGTH_LONG).show()
        }
    }
} 