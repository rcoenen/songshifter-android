package com.musicredirector

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import org.json.JSONObject
import android.webkit.CookieManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dedicated extractor for YouTube Music links.
 * Uses same approach as proven Node.js script with proper browser emulation.
 */
class YouTubeMusicExtractor(private val context: Context) {
    companion object {
        private const val TAG = "YouTubeMusicExtractor"
        private const val EXTRACTION_TIMEOUT_MS = 30000L // 30 seconds total timeout
        private const val INITIAL_DELAY_MS = 1000L // Wait for 1 second before starting extraction attempts
        private const val MAX_RETRIES = 10 // Maximum number of extraction attempts
        private const val RETRY_DELAY_MS = 500L // Wait between extraction attempts
        
        // Exact Chrome user agent that works
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        
        // First check if we're on a song page
        private const val PAGE_CHECK_SCRIPT = """
            (function() {
                try {
                    // Check if we're on a song page
                    const playerBar = document.querySelector('ytmusic-player-bar');
                    if (!playerBar) {
                        console.error('Not on a song page - player bar not found');
                        return 'NOT_READY';
                    }
                    
                    // Check if title is loaded
                    const title = document.querySelector('.title.style-scope.ytmusic-player-bar')?.textContent?.trim();
                    if (!title) {
                        console.error('Title element not found or empty');
                        return 'NOT_READY';
                    }
                    
                    // Check if artist is loaded
                    const artist = document.querySelector('.subtitle.style-scope.ytmusic-player-bar yt-formatted-string')?.textContent?.trim();
                    if (!artist) {
                        console.error('Artist element not found or empty');
                        return 'NOT_READY';
                    }
                    
                    return 'READY';
                } catch (e) {
                    console.error('Error checking page:', e);
                    return 'ERROR';
                }
            })();
        """
        
        // Only extract once we know the page is ready
        private const val EXTRACTION_SCRIPT = """
            (function() {
                try {
                    const metadata = {
                        title: document.querySelector('.title.style-scope.ytmusic-player-bar')?.textContent?.trim(),
                        artist: document.querySelector('.subtitle.style-scope.ytmusic-player-bar yt-formatted-string')?.textContent?.trim()
                    };
                    
                    console.log('Found metadata:', JSON.stringify(metadata));
                    return JSON.stringify(metadata);
                } catch (e) {
                    console.error('Extraction error:', e);
                    return null;
                }
            })();
        """
    }

    suspend fun extract(url: String): SongInfo? = withContext(Dispatchers.Main) {
        Log.d(TAG, "Starting extraction for URL: $url")
        
        try {
            // Validate URL
            if (!url.contains("music.youtube.com")) {
                Log.e(TAG, "Invalid YouTube Music URL")
                return@withContext null
            }
            
            return@withContext withTimeoutOrNull(EXTRACTION_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val hasExtracted = AtomicBoolean(false)
                    
                    try {
                        val webView = WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                
                                // Match working script's user agent exactly
                                userAgentString = USER_AGENT
                                
                                // Set desktop viewport size
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                
                                // Set initial scale to match desktop viewport
                                setInitialScale(100)
                                
                                // Clear cache and use no-cache mode
                                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            }
                            
                            // Clear cookies
                            CookieManager.getInstance().apply {
                                removeAllCookies(null)
                                flush()
                            }
                            
                            // Clear any existing cache
                            clearCache(true)
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    Log.d(TAG, "Page finished loading, starting extraction attempts")
                                    
                                    // Start checking if page is ready
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(INITIAL_DELAY_MS)
                                        attemptExtraction(view, continuation, hasExtracted, 0)
                                    }
                                }
                                
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    Log.e(TAG, "WebView error: ${error?.description}")
                                    if (!hasExtracted.get()) {
                                        hasExtracted.set(true)
                                        continuation.resume(null)
                                    }
                                }
                            }
                        }
                        
                        Log.d(TAG, "Loading URL with desktop Chrome emulation")
                        webView.loadUrl(url)
                        
                        continuation.invokeOnCancellation {
                            Log.d(TAG, "Extraction cancelled, cleaning up")
                            webView.destroy()
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in WebView setup: ${e.message}")
                        continuation.resume(null)
                    }
                }
            } ?: run {
                Log.e(TAG, "Extraction timed out after ${EXTRACTION_TIMEOUT_MS}ms")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during extraction: ${e.message}")
            null
        }
    }

    private fun attemptExtraction(
        webView: WebView?,
        continuation: CancellableContinuation<SongInfo?>,
        hasExtracted: AtomicBoolean,
        attempt: Int
    ) {
        if (attempt >= MAX_RETRIES || hasExtracted.get()) {
            if (!hasExtracted.get()) {
                Log.e(TAG, "Failed to extract after $MAX_RETRIES attempts")
                hasExtracted.set(true)
                continuation.resume(null)
            }
            return
        }
        
        // First check if the page is ready
        webView?.evaluateJavascript(PAGE_CHECK_SCRIPT) { result ->
            when (result.trim('"')) {
                "READY" -> {
                    Log.d(TAG, "Page is ready, extracting metadata")
                    extractMetadata(webView, continuation, hasExtracted)
                }
                "NOT_READY" -> {
                    Log.d(TAG, "Page not ready (attempt ${attempt + 1}/$MAX_RETRIES), retrying in ${RETRY_DELAY_MS}ms")
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(RETRY_DELAY_MS)
                        attemptExtraction(webView, continuation, hasExtracted, attempt + 1)
                    }
                }
                else -> {
                    Log.e(TAG, "Error checking page readiness")
                    if (!hasExtracted.get()) {
                        hasExtracted.set(true)
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private fun extractMetadata(
        webView: WebView?,
        continuation: CancellableContinuation<SongInfo?>,
        hasExtracted: AtomicBoolean
    ) {
        if (hasExtracted.get()) return
        
        webView?.evaluateJavascript(EXTRACTION_SCRIPT) { result ->
            if (hasExtracted.get()) return@evaluateJavascript
            
            try {
                Log.d(TAG, "JavaScript extraction result: $result")
                if (result != "null") {
                    val json = JSONObject(result.trim('"').replace("\\\"", "\""))
                    val title = json.optString("title", "").trim()
                    val artist = json.optString("artist", "").trim()
                    
                    if (title.isNotEmpty() && artist.isNotEmpty()) {
                        Log.d(TAG, "Successfully extracted - Title: \"$title\", Artist: \"$artist\"")
                        hasExtracted.set(true)
                        continuation.resume(SongInfo(title, artist))
                    } else {
                        Log.e(TAG, "Missing title or artist in extraction result")
                        hasExtracted.set(true)
                        continuation.resume(null)
                    }
                } else {
                    Log.e(TAG, "JavaScript extraction returned null")
                    hasExtracted.set(true)
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing extraction result: ${e.message}")
                hasExtracted.set(true)
                continuation.resume(null)
            }
        }
    }
} 