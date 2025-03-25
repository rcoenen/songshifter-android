package com.songshifter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
// Import for Android 11+ Domain Verification API
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState

/**
 * Manages domain link verification and app link handling
 */
class LinkVerificationManager(private val context: Context) {
    private val TAG = "LinkVerificationManager"
    
    /**
     * Opens app link settings for this app
     */
    fun openAppLinkSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if direct settings isn't available
            try {
                val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                fallbackIntent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(fallbackIntent)
                
                Toast.makeText(context, 
                    "Go to 'Open by default' and select 'Add links'", 
                    Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(context, 
                    "Couldn't open app settings. Please enable link handling manually.", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Checks if the domain is verified in Android settings.
     * 
     * This is a challenging problem because Android doesn't provide a reliable API to check if 
     * a domain is verified for an app, especially across different Android versions.
     * 
     * The most reliable way to check is to use PackageManager to query if our app can handle
     * these URLs, which is what we're doing here.
     */
    fun isDomainVerifiedInSettings(domain: String): Boolean {
        Log.d(TAG, "------------------- DOMAIN CHECK: $domain -------------------")
        
        try {
            // VERIFICATION TEST: Let's explicitly check what apps are registered for this domain
            val explicitTest = Intent(Intent.ACTION_VIEW, Uri.parse("https://$domain/test"))
            val allHandlers = context.packageManager.queryIntentActivities(explicitTest, 0)
            
            Log.d(TAG, "REGISTERED HANDLERS FOR $domain (count: ${allHandlers.size}):")
            allHandlers.forEachIndexed { index, info ->
                Log.d(TAG, "  [${index+1}] ${info.activityInfo.packageName}/${info.activityInfo.name}")
            }
            
            // Additional check: try with MATCH_ALL flag to see ALL handlers including non-default ones
            val allPossibleHandlers = context.packageManager.queryIntentActivities(explicitTest, PackageManager.MATCH_ALL)
            Log.d(TAG, "ALL POSSIBLE HANDLERS (MATCH_ALL) FOR $domain (count: ${allPossibleHandlers.size}):")
            allPossibleHandlers.forEachIndexed { index, info ->
                Log.d(TAG, "  [${index+1}] ${info.activityInfo.packageName}/${info.activityInfo.name}")
            }
            
            // Check if our app is in the handlers list (through manifest intent filters)
            val isOurAppInHandlers = allHandlers.any { 
                it.activityInfo.packageName == context.packageName &&
                it.activityInfo.name.contains("Redirect", ignoreCase = true)
            }
            
            // Flag to track if this domain is properly verified in Android 11+ settings
            var isVerifiedInSettings = false
            
            // Check domain verification status (Android 11+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val domainVerificationManager = context.getSystemService(DomainVerificationManager::class.java)
                    val userState = domainVerificationManager.getDomainVerificationUserState(context.packageName)
                    userState?.let { state ->
                        val domainStatuses = state.hostToStateMap
                        Log.d(TAG, "ANDROID 11+ DOMAIN VERIFICATION STATUS:")
                        domainStatuses.forEach { (host, status) ->
                            val statusString = when (status) {
                                DomainVerificationUserState.DOMAIN_STATE_VERIFIED -> "VERIFIED"
                                DomainVerificationUserState.DOMAIN_STATE_SELECTED -> "SELECTED"
                                DomainVerificationUserState.DOMAIN_STATE_NONE -> "NONE"
                                else -> "UNKNOWN ($status)"
                            }
                            Log.d(TAG, "  Domain: $host Status: $statusString")
                            if (host.contains(domain)) {
                                Log.d(TAG, "✓✓✓ FOUND TARGET DOMAIN IN VERIFICATION MAP: $host with status $statusString")
                                // Consider the domain verified if it's either VERIFIED or SELECTED in settings
                                if (status == DomainVerificationUserState.DOMAIN_STATE_VERIFIED || 
                                    status == DomainVerificationUserState.DOMAIN_STATE_SELECTED) {
                                    isVerifiedInSettings = true
                                    Log.d(TAG, "✓✓✓ Domain is properly configured in Android settings (status=$statusString)")
                                }
                            }
                        }
                    } ?: Log.d(TAG, "Domain verification state is null")
                } catch (e: Exception) {
                    Log.d(TAG, "Error getting domain verification status: ${e.message}")
                }
            } else {
                // On older Android versions, we rely only on the manifest-based handler check
                isVerifiedInSettings = isOurAppInHandlers
            }
            
            // Now let's check what happens when we try to open this domain without a specific app
            val defaultHandler = context.packageManager.resolveActivity(explicitTest, 0)
            if (defaultHandler != null) {
                Log.d(TAG, "DEFAULT HANDLER: ${defaultHandler.activityInfo.packageName}/${defaultHandler.activityInfo.name}")
            } else {
                Log.d(TAG, "NO DEFAULT HANDLER FOUND")
            }
            
            // On Android 11+, both conditions must be true:
            // 1. Our app must be in the handlers list (from manifest)
            // 2. The domain must be verified/selected in Android settings
            //
            // On older Android versions, just check the handlers list
            val isVerified = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                isOurAppInHandlers && isVerifiedInSettings
            } else {
                isOurAppInHandlers
            }
            
            if (isVerified) {
                Log.d(TAG, "✓ SUCCESS: Our app is registered and verified to handle $domain links")
                return true
            } else {
                if (isOurAppInHandlers && !isVerifiedInSettings) {
                    Log.d(TAG, "✗ PARTIAL: Our app is registered in manifest but NOT verified in settings for $domain links")
                } else if (!isOurAppInHandlers && isVerifiedInSettings) {
                    Log.d(TAG, "⚠️ UNUSUAL: Domain is selected in settings but not registered in manifest for $domain links")
                    // Still consider this verified since it's selected in Android settings
                    return true
                } else {
                    Log.d(TAG, "✗ FAILURE: Our app is NOT registered to handle $domain links")
                }
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking domain verification: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Disables this app as a handler for YouTube Music URLs
     * This should be called when in Spotify to YouTube Music mode to prevent circular redirections
     * On Android 10+, this modifies the domain verification settings for the app
     */
    fun disableYouTubeMusicHandling() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "🔥 Disabling YouTube Music link handling for this app")
            try {
                val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                
                // Show instructions to the user
                Toast.makeText(
                    context,
                    "To prevent circular redirection, please disable music.youtube.com handling. " +
                    "Tap 'Open by default' then uncheck music.youtube.com",
                    Toast.LENGTH_LONG
                ).show()
                
                // Open the settings
                context.startActivity(intent)
                
                // Log in-app instruction for tracking
                Log.d(TAG, "✓ Opened app link settings to disable YouTube Music handling")
                return
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error opening app link settings: ${e.message}")
            }
        }
        
        // Fallback for older Android versions or if settings couldn't be opened
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
            
            Toast.makeText(
                context,
                "To prevent circular redirection, please go to 'Open by default' and disable handling of music.youtube.com links",
                Toast.LENGTH_LONG
            ).show()
            
            Log.d(TAG, "✓ Opened app details settings as fallback")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening app details settings: ${e.message}")
            
            // Last resort: Show a toast explaining what to do manually
            Toast.makeText(
                context,
                "Please go to Settings > Apps > Music Redirector > Open by default and disable music.youtube.com handling",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Checks if we need to disable YouTube Music link handling based on the current mode
     * Returns true if handling was already properly set, false if user needs to change settings
     */
    fun ensureCorrectLinkHandling(preferredPlatform: String): Boolean {
        val ytMusicDomain = "music.youtube.com"
        val spotifyDomain = "open.spotify.com"
        
        // In Spotify to YouTube Music mode, we should NOT handle music.youtube.com links
        if (preferredPlatform == PreferencesHelper.PLATFORM_YOUTUBE_MUSIC) {
            // Check if we're currently handling YouTube Music links
            val isHandlingYtMusic = isDomainVerifiedInSettings(ytMusicDomain)
            
            if (isHandlingYtMusic) {
                Log.d(TAG, "⚠️ We're in Spotify→YT Music mode but still handling music.youtube.com links")
                return false // User needs to change settings
            } else {
                Log.d(TAG, "✓ Correctly NOT handling music.youtube.com links in Spotify→YT Music mode")
                return true // All good
            }
        }
        // In YouTube Music to Spotify mode, we should handle both domains
        else if (preferredPlatform == PreferencesHelper.PLATFORM_SPOTIFY) {
            // We should be handling both domains
            val isHandlingYtMusic = isDomainVerifiedInSettings(ytMusicDomain)
            val isHandlingSpotify = isDomainVerifiedInSettings(spotifyDomain)
            
            if (!isHandlingYtMusic || !isHandlingSpotify) {
                Log.d(TAG, "⚠️ We're in YT Music→Spotify mode but not handling all required domains")
                return false // User needs to change settings
            } else {
                Log.d(TAG, "✓ Correctly handling all domains in YT Music→Spotify mode")
                return true // All good
            }
        }
        
        return true // Default to true for any other case
    }
} 