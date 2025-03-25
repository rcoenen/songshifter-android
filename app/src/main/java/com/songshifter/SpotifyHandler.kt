package com.songshifter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast

/**
 * Handler for Spotify app specific functionality
 */
class SpotifyHandler(private val context: Context) {
    private val TAG = "SpotifyHandler"
    private val SPOTIFY_PACKAGE = "com.spotify.music"
    
    /**
     * Checks if Spotify app is installed on the device
     */
    fun isSpotifyInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Checks if Spotify can be uninstalled
     * Unlike YouTube Music which is often a system app, Spotify can usually be uninstalled
     */
    fun isSpotifyUninstallable(): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(SPOTIFY_PACKAGE, 0)
            // If the app is not a system app, it can be uninstalled
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if Spotify is uninstallable: ${e.message}")
            // Default to true since Spotify is typically uninstallable
            true
        }
    }
    
    /**
     * Checks if Spotify is handling its own links.
     * The app can't handle links if it's not installed, so this returns false in that case.
     * Otherwise, checks if Spotify is in the list of handlers for open.spotify.com URLs.
     */
    fun isSpotifyHandlingLinks(): Boolean {
        // Spotify can't handle links if it's not installed
        if (!isSpotifyInstalled()) {
            return false
        }
        
        // Otherwise, check if it's in the list of handlers
        try {
            val pm = context.packageManager
            val testUri = Uri.parse("https://open.spotify.com/track/test")
            val testIntent = Intent(Intent.ACTION_VIEW, testUri)
            
            // Query for activities that can handle this URI
            val resolveInfoList = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Check if Spotify is in the list of handlers
            return resolveInfoList.any { resolveInfo ->
                resolveInfo.activityInfo.packageName == SPOTIFY_PACKAGE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if Spotify handles links: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Opens Spotify app settings
     */
    fun openSpotifySettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", SPOTIFY_PACKAGE, null)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, 
                "Couldn't open Spotify settings. Please uninstall Spotify manually.", 
                Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Opens app info page for Spotify to let user uninstall it
     */
    fun openSpotifyUninstall() {
        // First verify Spotify is actually installed
        if (!isSpotifyInstalled()) {
            Log.i(TAG, "✓ Spotify is not installed, nothing to uninstall")
            Toast.makeText(context, 
                "Spotify is not installed on this device.",
                Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Opening Spotify app info for uninstall...")
        
        // Direct approach: Open app details settings page where user can uninstall
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", SPOTIFY_PACKAGE, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            // Show instructions
            Toast.makeText(
                context,
                "Tap 'Uninstall' to remove Spotify",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "✅ Opened app details settings for Spotify")
            return
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening app settings: ${e.message}")
            e.printStackTrace()
        }
        
        // Fallback 1: Use ACTION_DELETE if app info page fails
        try {
            val packageURI = Uri.parse("package:$SPOTIFY_PACKAGE")
            val deleteIntent = Intent(Intent.ACTION_DELETE, packageURI)
            deleteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(deleteIntent)
            Log.d(TAG, "✅ Started ACTION_DELETE intent for Spotify")
            
            // Show confirmation toast
            Toast.makeText(
                context,
                "Confirm uninstall when prompted",
                Toast.LENGTH_LONG
            ).show()
            return
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error using ACTION_DELETE: ${e.message}")
            e.printStackTrace()
        }
        
        // Fallback 2: Try to show general app settings
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            Toast.makeText(
                context,
                "Find Spotify in the list and tap to uninstall",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "✅ Opened general app settings")
            return
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening app manager settings: ${e.message}")
            e.printStackTrace()
        }
        
        // If all methods failed, show diagnostic info
        Toast.makeText(
            context,
            "Unable to open Spotify settings. Please uninstall it manually from your device settings.",
            Toast.LENGTH_LONG
        ).show()
        Log.e(TAG, "❌ All uninstall approaches failed")
    }
} 