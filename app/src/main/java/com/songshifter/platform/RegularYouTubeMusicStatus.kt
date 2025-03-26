package com.songshifter.platform

import android.content.Context
import android.view.View
import android.widget.Toast

/**
 * Implementation for non-Pixel devices where YouTube Music is a regular app
 * In this case, we need to ensure YouTube Music is INSTALLED
 */
class RegularYouTubeMusicStatus(context: Context) : BaseYouTubeMusicStatus(context) {
    
    /**
     * On regular devices, YouTube Music should be INSTALLED
     */
    override fun isInCorrectState(): Boolean {
        // YouTube Music should be installed and enabled
        return isInstalled() && isEnabled()
    }
    
    override fun getStepNumber(): String {
        return "2"
    }
    
    override fun getSuccessMessage(): String {
        return "${getStepNumber()}. ✓ YouTube Music app is installed"
    }
    
    override fun getActionNeededMessage(): String {
        return "${getStepNumber()}. ✗ YouTube Music app needs to be installed (tap to fix)"
    }
    
    override fun executeFixAction(view: View) {
        openPlayStoreForYouTubeMusic()
        
        // Show toast with instructions
        Toast.makeText(
            context,
            "Please install YouTube Music from the Play Store",
            Toast.LENGTH_LONG
        ).show()
    }
} 