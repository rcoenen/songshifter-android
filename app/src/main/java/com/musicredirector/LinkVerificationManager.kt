package com.musicredirector

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
} 