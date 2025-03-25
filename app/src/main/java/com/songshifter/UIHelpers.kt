package com.songshifter

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Helper class for UI-related functionality
 */
class UIHelpers(
    private val context: Context,
    private val youTubeMusicHandler: YouTubeMusicHandler,
    private val spotifyHandler: SpotifyHandler
) {
    private var youTubeMusicWarningDialog: AlertDialog? = null
    private var youTubeMusicDisabledWarningDialog: AlertDialog? = null
    private var spotifyWarningDialog: AlertDialog? = null
    
    /**
     * Shows a warning dialog about YouTube Music link handling
     */
    fun showYouTubeMusicWarningDialog() {
        // First check if YouTube Music is disabled
        if (youTubeMusicHandler.isYouTubeMusicDisabled()) {
            return
        }
        
        // Then check if it's handling links
        if (!youTubeMusicHandler.isYouTubeMusicHandlingLinks()) {
            return
        }
        
        // Dismiss any existing dialog
        youTubeMusicWarningDialog?.dismiss()
        
        youTubeMusicWarningDialog = MaterialAlertDialogBuilder(context)
            .setTitle("⚠️ Action Required")
            .setMessage("Since you've selected Spotify as your preferred platform, you need to disable YouTube Music's link handling to ensure links open in Spotify.\n\nWould you like to open YouTube Music's settings now?")
            .setPositiveButton("Open Settings") { dialog, _ ->
                try {
                    youTubeMusicHandler.openYouTubeMusicSettings()
                } catch (e: Exception) {
                    Toast.makeText(context, 
                        "Couldn't open YouTube Music settings. Please disable link handling manually.", 
                        Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ -> 
                dialog.dismiss()
            }
            .setCancelable(true)  // Make dialog cancelable
            .create()
            
        youTubeMusicWarningDialog?.show()
    }
    
    /**
     * Shows a warning dialog when YouTube Music is selected as the preferred platform
     * but the YouTube Music app is disabled.
     */
    fun showYouTubeMusicDisabledWarningDialog() {
        // Get current preferred platform
        val preferencesHelper = PreferencesHelper(context)
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // First check if YouTube Music is actually disabled
        if (!youTubeMusicHandler.isYouTubeMusicDisabled()) {
            return
        }
        
        // Only show this warning if YouTube Music is the selected platform
        if (preferredPlatform != PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
            return
        }
        
        // Dismiss any existing dialog
        youTubeMusicDisabledWarningDialog?.dismiss()
        
        youTubeMusicDisabledWarningDialog = MaterialAlertDialogBuilder(context)
            .setTitle("⚠️ YouTube Music Disabled")
            .setMessage("You've selected YouTube Music as your preferred platform, but the YouTube Music app is currently disabled. You need to enable it for the app to function properly.\n\nWould you like to open YouTube Music's settings now?")
            .setPositiveButton("Open Settings") { dialog, _ ->
                try {
                    youTubeMusicHandler.openYouTubeMusicSettings()
                } catch (e: Exception) {
                    Toast.makeText(context, 
                        "Couldn't open YouTube Music settings. Please enable it manually.", 
                        Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ -> 
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            
        youTubeMusicDisabledWarningDialog?.show()
    }
    
    /**
     * Shows a warning dialog about Spotify being installed when YouTube Music is the preferred platform
     */
    fun showSpotifyWarningDialog() {
        // First check if Spotify is installed
        if (!spotifyHandler.isSpotifyInstalled()) {
            return
        }
        
        // Then check if it's handling links
        if (!spotifyHandler.isSpotifyHandlingLinks()) {
            return
        }
        
        // Get current preferred platform
        val preferencesHelper = PreferencesHelper(context)
        val preferredPlatform = preferencesHelper.getPreferredPlatform()
        
        // Only show this warning if YouTube Music is the selected platform
        if (preferredPlatform != PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
            return
        }
        
        // Dismiss any existing dialog
        spotifyWarningDialog?.dismiss()
        
        spotifyWarningDialog = MaterialAlertDialogBuilder(context)
            .setTitle("⚠️ Spotify Installed")
            .setMessage("You've selected YouTube Music as your preferred platform, but Spotify is currently installed. Android prioritizes the Spotify app for handling Spotify links.\n\nTo enable redirection of Spotify links to YouTube Music, you need to uninstall Spotify.\n\nWould you like to uninstall Spotify now?")
            .setPositiveButton("Uninstall Spotify") { dialog, _ ->
                try {
                    spotifyHandler.openSpotifyUninstall()
                } catch (e: Exception) {
                    Toast.makeText(context, 
                        "Couldn't open uninstall screen. Please uninstall Spotify manually.", 
                        Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNeutralButton("Settings") { dialog, _ ->
                try {
                    spotifyHandler.openSpotifySettings()
                } catch (e: Exception) {
                    Toast.makeText(context, 
                        "Couldn't open Spotify settings.", 
                        Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ -> 
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            
        spotifyWarningDialog?.show()
    }
    
    /**
     * Dismisses the YouTube Music warning dialog if it's showing
     */
    fun dismissYouTubeMusicWarningDialog() {
        youTubeMusicWarningDialog?.dismiss()
        youTubeMusicWarningDialog = null
    }
    
    /**
     * Dismisses the YouTube Music disabled warning dialog if it's showing
     */
    fun dismissYouTubeMusicDisabledWarningDialog() {
        youTubeMusicDisabledWarningDialog?.dismiss()
        youTubeMusicDisabledWarningDialog = null
    }
    
    /**
     * Dismisses the Spotify warning dialog if it's showing
     */
    fun dismissSpotifyWarningDialog() {
        spotifyWarningDialog?.dismiss()
        spotifyWarningDialog = null
    }
} 