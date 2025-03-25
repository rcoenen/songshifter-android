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
import android.app.Activity

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

    private var extractionCompleted = false

    suspend fun extract(url: String): SongInfo? = withContext(Dispatchers.Main) {
        Log.d(TAG, "Starting extraction for URL: $url")
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled = true
                userAgentString = USER_AGENT
                domStorageEnabled = true
            }
            
            val hasExtracted = AtomicBoolean(false)
            extractionCompleted = false
            
            webView.webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    // Only log errors if extraction hasn't completed yet
                    if (!extractionCompleted && !hasExtracted.get()) {
                        Log.e(TAG, "WebView error: ${error?.description}")
                        if (!hasExtracted.get()) {
                            hasExtracted.set(true)
                            continuation.resume(null)
                        }
                    }
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(TAG, "Page finished loading, starting extraction attempts")
                    startExtractionAttempts(webView, hasExtracted, continuation)
                }
            }
            
            Log.d(TAG, "Loading URL with desktop Chrome emulation")
            webView.loadUrl(url)
            
            continuation.invokeOnCancellation {
                webView.stopLoading()
                (context as? Activity)?.runOnUiThread {
                    webView.destroy()
                }
            }
        }
    }

    private fun startExtractionAttempts(
        webView: WebView,
        hasExtracted: AtomicBoolean,
        continuation: CancellableContinuation<SongInfo?>
    ) {
        // First check if the page is ready
        webView.evaluateJavascript(PAGE_CHECK_SCRIPT) { result ->
            when (result.trim('"')) {
                "READY" -> {
                    Log.d(TAG, "Page is ready, extracting metadata")
                    extractMetadata(webView, hasExtracted, continuation)
                }
                "NOT_READY" -> {
                    Log.d(TAG, "Page not ready, retrying in ${RETRY_DELAY_MS}ms")
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(RETRY_DELAY_MS)
                        startExtractionAttempts(webView, hasExtracted, continuation)
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
        webView: WebView,
        hasExtracted: AtomicBoolean,
        continuation: CancellableContinuation<SongInfo?>
    ) {
        if (hasExtracted.get()) return
        
        webView.evaluateJavascript(EXTRACTION_SCRIPT) { result ->
            if (hasExtracted.get()) return@evaluateJavascript
            
            try {
                Log.d(TAG, "JavaScript extraction result: $result")
                if (result != "null") {
                    handleExtractionSuccess(webView, hasExtracted, continuation, result)
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

    private fun handleExtractionSuccess(
        webView: WebView,
        hasExtracted: AtomicBoolean,
        continuation: CancellableContinuation<SongInfo?>,
        result: String
    ) {
        if (!hasExtracted.get()) {
            try {
                val json = JSONObject(result)
                val title = json.getString("title")
                val artist = json.getString("artist")
                
                Log.d(TAG, "Successfully extracted - Title: \"$title\", Artist: \"$artist\"")
                
                hasExtracted.set(true)
                extractionCompleted = true
                continuation.resume(SongInfo(title = title, artist = artist))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing extraction result: ${e.message}")
                if (!hasExtracted.get()) {
                    hasExtracted.set(true)
                    continuation.resume(null)
                }
            }
        }
    }
} 