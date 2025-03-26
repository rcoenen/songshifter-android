package com.songshifter.platform

import android.view.View

/**
 * Interface for handling platform-specific YouTube Music status and actions
 * This allows different behaviors for Pixel phones (system app) and other phones
 */
interface YouTubeMusicStatus {
    /**
     * Package name of YouTube Music app
     */
    val YOUTUBE_MUSIC_PACKAGE: String
        get() = "com.google.android.apps.youtube.music"
    
    /**
     * Checks if YouTube Music is in the correct state for the current device.
     * For Pixel (system app): should be disabled
     * For regular phones: should be installed
     */
    fun isInCorrectState(): Boolean
    
    /**
     * Gets the step number for display in UI
     */
    fun getStepNumber(): String
    
    /**
     * Gets status message for success state
     */
    fun getSuccessMessage(): String
    
    /**
     * Gets status message for action needed state
     */
    fun getActionNeededMessage(): String
    
    /**
     * Fix action to take when YouTube Music is in incorrect state
     */
    fun executeFixAction(view: View)
    
    /**
     * Check if YouTube Music is installed on the device
     */
    fun isInstalled(): Boolean
    
    /**
     * Check if YouTube Music is enabled
     */
    fun isEnabled(): Boolean
    
    /**
     * Check if YouTube Music is handling its own links
     */
    fun isHandlingLinks(): Boolean
} 