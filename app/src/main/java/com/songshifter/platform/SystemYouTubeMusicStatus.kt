package com.songshifter.platform

import android.content.Context
import android.view.View
import android.widget.Toast

/**
 * Implementation for Pixel devices where YouTube Music is a system app
 * In this case, we need to ENABLE YouTube Music, not uninstall it
 */
class SystemYouTubeMusicStatus(context: Context) : BaseYouTubeMusicStatus(context) {
    
    /**
     * On Pixel devices, YouTube Music should be ENABLED
     */
    override fun isInCorrectState(): Boolean {
        // YouTube Music should be installed (as a system app) and ENABLED
        return isInstalled() && isEnabled()
    }
    
    override fun getStepNumber(): String {
        return "1"
    }
    
    override fun getSuccessMessage(): String {
        return "${getStepNumber()}. ✓ YouTube Music app is enabled"
    }
    
    override fun getActionNeededMessage(): String {
        return "${getStepNumber()}. ✗ YouTube Music app needs to be enabled (tap to fix)"
    }
    
    override fun executeFixAction(view: View) {
        openYouTubeMusicSettings()
        
        // Show toast with instructions specific to enabling
        Toast.makeText(
            context,
            "In YouTube Music settings: tap 'Enable' to allow it to handle links",
            Toast.LENGTH_LONG
        ).show()
    }
} 