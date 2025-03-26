package com.songshifter.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Base implementation for YouTube Music status with common functionality
 */
abstract class BaseYouTubeMusicStatus(protected val context: Context) : YouTubeMusicStatus {
    
    protected val TAG = "YouTubeMusicStatus"
    
    /**
     * Check if YouTube Music is installed
     */
    override fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(YOUTUBE_MUSIC_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Check if YouTube Music is enabled
     */
    override fun isEnabled(): Boolean {
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
     * Check if YouTube Music is handling its own links
     */
    override fun isHandlingLinks(): Boolean {
        // YouTube Music can't handle links if it's disabled
        if (!isEnabled()) {
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
            return false
        }
    }
    
    /**
     * Open YouTube Music app settings
     */
    protected fun openYouTubeMusicSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", YOUTUBE_MUSIC_PACKAGE, null)
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening YouTube Music settings: ${e.message}")
        }
    }
    
    /**
     * Open Play Store to install YouTube Music
     */
    protected fun openPlayStoreForYouTubeMusic() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$YOUTUBE_MUSIC_PACKAGE")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser if Play Store isn't available
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$YOUTUBE_MUSIC_PACKAGE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open YouTube Music in Play Store or browser: ${e2.message}")
            }
        }
    }
} 