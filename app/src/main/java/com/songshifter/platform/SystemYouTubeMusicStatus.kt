package com.songshifter.platform

import android.content.Context
import android.view.View
import android.widget.Toast

/**
 * Implementation for Pixel devices where YouTube Music is a system app
 * In this case, we need to DISABLE YouTube Music rather than uninstall it
 */
class SystemYouTubeMusicStatus(context: Context) : BaseYouTubeMusicStatus(context) {
    
    /**
     * On Pixel devices, YouTube Music should be DISABLED
     */
    override fun isInCorrectState(): Boolean {
        // YouTube Music should be installed (as a system app) but DISABLED
        return isInstalled() && !isEnabled()
    }
    
    override fun getStepNumber(): String {
        return "2"
    }
    
    override fun getSuccessMessage(): String {
        return "${getStepNumber()}. ✓ YouTube Music app is disabled"
    }
    
    override fun getActionNeededMessage(): String {
        return "${getStepNumber()}. ✗ YouTube Music app needs to be disabled (tap to fix)"
    }
    
    override fun executeFixAction(view: View) {
        openYouTubeMusicSettings()
        
        // Show toast with instructions specific to disabling
        Toast.makeText(
            context,
            "In YouTube Music settings: tap 'Disable' to prevent it from handling links",
            Toast.LENGTH_LONG
        ).show()
    }
} 