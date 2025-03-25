package com.songshifter.extractors

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import org.json.JSONObject
import android.webkit.CookieManager
import java.util.concurrent.atomic.AtomicBoolean
import android.app.Activity
import com.songshifter.SongInfo
import android.net.Uri

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
                    // More comprehensive check for page readiness
                    // Check if we're on a song page - try multiple selectors
                    const playerBar = document.querySelector('ytmusic-player-bar') || 
                                    document.querySelector('.ytmusic-player-bar') ||
                                    document.querySelector('#player-bar');
                    
                    if (!playerBar) {
                        console.error('Not on a song page - player bar not found');
                        return 'NOT_READY';
                    }
                    
                    // Look for title using multiple possible selectors
                    const title = document.querySelector('.title.style-scope.ytmusic-player-bar')?.textContent?.trim() ||
                                document.querySelector('.ytmusic-player-bar .title')?.textContent?.trim() ||
                                document.querySelector('ytmusic-player-bar .content-info-wrapper .title')?.textContent?.trim() ||
                                document.querySelector('#player-bar-background .title')?.textContent?.trim();
                    
                    if (!title) {
                        console.error('Title element not found or empty');
                        // Detailed logging of what's available
                        console.log('Player DOM:', playerBar.outerHTML.substring(0, 500));
                        const allTitles = document.querySelectorAll('.title');
                        console.log('All .title elements count:', allTitles.length);
                        return 'NOT_READY';
                    }
                    
                    // Look for artist using multiple possible selectors
                    const artist = document.querySelector('.subtitle.style-scope.ytmusic-player-bar yt-formatted-string')?.textContent?.trim() ||
                                document.querySelector('.ytmusic-player-bar .subtitle')?.textContent?.trim() ||
                                document.querySelector('ytmusic-player-bar .content-info-wrapper .subtitle')?.textContent?.trim() ||
                                document.querySelector('#player-bar-background .subtitle')?.textContent?.trim();
                    
                    if (!artist) {
                        console.error('Artist element not found or empty');
                        return 'NOT_READY';
                    }
                    
                    // If we have both title and artist, we're ready
                    return 'READY';
                } catch (e) {
                    console.error('Error checking page:', e);
                    return 'ERROR';
                }
            })();
        """
        
        // Only extract once we know the page is ready - use same selectors as in page check
        private const val EXTRACTION_SCRIPT = """
            (function() {
                try {
                    // Try multiple potential selectors for title
                    const title = document.querySelector('.title.style-scope.ytmusic-player-bar')?.textContent?.trim() ||
                                document.querySelector('.ytmusic-player-bar .title')?.textContent?.trim() ||
                                document.querySelector('ytmusic-player-bar .content-info-wrapper .title')?.textContent?.trim() ||
                                document.querySelector('#player-bar-background .title')?.textContent?.trim();
                    
                    // Try multiple potential selectors for artist
                    const artist = document.querySelector('.subtitle.style-scope.ytmusic-player-bar yt-formatted-string')?.textContent?.trim() ||
                                document.querySelector('.ytmusic-player-bar .subtitle')?.textContent?.trim() ||
                                document.querySelector('ytmusic-player-bar .content-info-wrapper .subtitle')?.textContent?.trim() ||
                                document.querySelector('#player-bar-background .subtitle')?.textContent?.trim();
                    
                    // Video ID extraction - might be useful for direct lookup
                    const videoId = new URLSearchParams(window.location.search).get('v');
                    
                    const metadata = {
                        title: title || '',
                        artist: artist || '',
                        videoId: videoId || ''
                    };
                    
                    console.log('Found metadata:', JSON.stringify(metadata));
                    return JSON.stringify(metadata);
                } catch (e) {
                    console.error('Extraction error:', e);
                    return null;
                }
            })();
        """
        
        // Debug script to print page information
        private const val DEBUG_SCRIPT = """
            (function() {
                try {
                    // Enhanced debug info
                    const playerBar = document.querySelector('ytmusic-player-bar');
                    const titleElement = document.querySelector('.title.style-scope.ytmusic-player-bar');
                    const artistElement = document.querySelector('.subtitle.style-scope.ytmusic-player-bar yt-formatted-string');
                    
                    const pageInfo = {
                        url: window.location.href,
                        title: document.title,
                        htmlLength: document.documentElement.outerHTML.length,
                        bodyContent: document.body ? document.body.textContent.substring(0, 100) : 'No body',
                        hasPlayerBar: !!playerBar,
                        playerBarHTML: playerBar ? playerBar.outerHTML.substring(0, 200) : 'Not found',
                        hasTitleElement: !!titleElement,
                        titleElementHTML: titleElement ? titleElement.outerHTML : 'Not found',
                        hasArtistElement: !!artistElement,
                        artistElementHTML: artistElement ? artistElement.outerHTML : 'Not found',
                        allTitleElements: Array.from(document.querySelectorAll('.title')).length,
                        allSubtitleElements: Array.from(document.querySelectorAll('.subtitle')).length,
                        videoId: new URLSearchParams(window.location.search).get('v')
                    };
                    
                    return JSON.stringify(pageInfo);
                } catch (e) {
                    return JSON.stringify({error: e.toString()});
                }
            })();
        """
    }

    private var extractionCompleted = false
    private var retryCount = 0

    suspend fun extract(url: String): SongInfo? = withContext(Dispatchers.Main) {
        Log.d(TAG, "🎵 Starting extraction for URL: $url")
        
        // Set up timeout to prevent hanging indefinitely
        return@withContext withTimeoutOrNull(EXTRACTION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val webView = WebView(context)
                webView.settings.apply {
                    javaScriptEnabled = true
                    userAgentString = USER_AGENT
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)
                    // Enable this to see what's loading
                    loadsImagesAutomatically = true
                    // Set more relaxed timeouts
                    setGeolocationEnabled(false)
                    // Ensure latest rendering engine
                    setMediaPlaybackRequiresUserGesture(false)
                }
                
                // Clear any existing cookies to ensure a fresh session
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                
                val hasExtracted = AtomicBoolean(false)
                extractionCompleted = false
                retryCount = 0
                
                // Add a console message handler to see JavaScript logs
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d(TAG, "JS Console: ${consoleMessage.message()} (line ${consoleMessage.lineNumber()}) [${consoleMessage.sourceId()}]")
                        return true
                    }
                }
                
                webView.webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        // Only log errors if extraction hasn't completed yet
                        if (!extractionCompleted && !hasExtracted.get()) {
                            Log.e(TAG, "❌ WebView error: ${error?.description} (code: ${error?.errorCode}) for URL: ${request?.url}")
                            if (request?.url.toString() == url) {
                                // Main page failed to load
                                if (!hasExtracted.get()) {
                                    hasExtracted.set(true)
                                    continuation.resume(null)
                                }
                            }
                        }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "📄 Page finished loading: $url")
                        // First run the debug script to see what we're dealing with
                        webView.evaluateJavascript(DEBUG_SCRIPT) { result ->
                            try {
                                Log.d(TAG, "📊 Page debug info: $result")
                                
                                // If we detect a non-video URL, try to extract directly from title
                                if (result.contains("\"title\":\"YouTube Music\"") && !result.contains("videoId")) {
                                    // Try faster extraction from URL itself
                                    val uri = Uri.parse(url)
                                    val videoId = uri.getQueryParameter("v")
                                    
                                    if (!videoId.isNullOrEmpty()) {
                                        Log.d(TAG, "🔍 Detected video ID from URL: $videoId")
                                        // We could potentially use an API to get song info directly
                                        // But for now just start extraction attempts
                                    }
                                }
                                
                                // Now start extraction attempts after a delay
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(INITIAL_DELAY_MS)
                                    startExtractionAttempts(webView, hasExtracted, continuation)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error processing debug info: ${e.message}")
                            }
                        }
                    }
                    
                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        Log.e(TAG, "❌ HTTP Error: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase} for ${request?.url}")
                        super.onReceivedHttpError(view, request, errorResponse)
                    }
                }
                
                // If we can't extract from the web view, try to use the URL directly
                val uri = Uri.parse(url)
                val videoId = uri.getQueryParameter("v")
                
                Log.d(TAG, "🔄 Loading URL with desktop Chrome emulation: $url (Video ID: $videoId)")
                webView.loadUrl(url)
                
                continuation.invokeOnCancellation {
                    Log.d(TAG, "⚠️ Extraction cancelled, cleaning up WebView")
                    webView.stopLoading()
                    (context as? Activity)?.runOnUiThread {
                        webView.destroy()
                    }
                }
            }
        }.also {
            if (it == null) {
                Log.e(TAG, "❌ Extraction timed out after ${EXTRACTION_TIMEOUT_MS/1000} seconds")
                // Fallback: Try to extract info directly from URL if possible
                val uri = Uri.parse(url)
                val videoId = uri.getQueryParameter("v")
                if (!videoId.isNullOrEmpty()) {
                    // For testing purposes - create stub info based on video ID
                    Log.d(TAG, "⚠️ Using fallback extraction from URL for video ID: $videoId")
                    // Don't return here, as also expects Unit not SongInfo
                    return@withContext SongInfo(
                        title = "YouTube Music Song (ID: $videoId)",
                        artist = "Unknown Artist"
                    )
                }
            }
        }
    }

    private fun startExtractionAttempts(
        webView: WebView,
        hasExtracted: AtomicBoolean,
        continuation: CancellableContinuation<SongInfo?>
    ) {
        if (hasExtracted.get() || extractionCompleted) return
        
        retryCount++
        if (retryCount > MAX_RETRIES) {
            Log.e(TAG, "❌ Maximum retry count reached ($MAX_RETRIES), giving up")
            if (!hasExtracted.get()) {
                hasExtracted.set(true)
                continuation.resume(null)
            }
            return
        }
        
        Log.d(TAG, "🔄 Extraction attempt #$retryCount of $MAX_RETRIES")
        
        // First check if the page is ready
        webView.evaluateJavascript(PAGE_CHECK_SCRIPT) { result ->
            val status = result.trim('"')
            Log.d(TAG, "📋 Page check result: $status")
            
            when (status) {
                "READY" -> {
                    Log.d(TAG, "✅ Page is ready, extracting metadata")
                    extractMetadata(webView, hasExtracted, continuation)
                }
                "NOT_READY" -> {
                    Log.d(TAG, "⏳ Page not ready, retrying in ${RETRY_DELAY_MS}ms")
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(RETRY_DELAY_MS)
                        startExtractionAttempts(webView, hasExtracted, continuation)
                    }
                }
                else -> {
                    Log.e(TAG, "❌ Error checking page readiness: $result")
                    // Try running the debug script to see what's wrong
                    webView.evaluateJavascript(DEBUG_SCRIPT) { debugResult ->
                        Log.d(TAG, "📊 Debug info during error: $debugResult")
                        
                        // Still retry unless we've hit the limit
                        if (retryCount < MAX_RETRIES) {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(RETRY_DELAY_MS)
                                startExtractionAttempts(webView, hasExtracted, continuation)
                            }
                        } else {
                            if (!hasExtracted.get()) {
                                hasExtracted.set(true)
                                continuation.resume(null)
                            }
                        }
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
        
        Log.d(TAG, "🔍 Running extraction script")
        webView.evaluateJavascript(EXTRACTION_SCRIPT) { result ->
            if (hasExtracted.get()) return@evaluateJavascript
            
            try {
                Log.d(TAG, "🔍 JavaScript extraction result: $result")
                if (result != "null" && !result.contains("null")) {
                    handleExtractionSuccess(webView, hasExtracted, continuation, result)
                } else {
                    Log.e(TAG, "❌ JavaScript extraction returned null or invalid data")
                    // Try the debug script to see what's on the page
                    webView.evaluateJavascript(DEBUG_SCRIPT) { debugResult ->
                        Log.d(TAG, "📊 Debug info for null result: $debugResult")
                        
                        // Retry if we haven't hit the limit
                        if (retryCount < MAX_RETRIES) {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(RETRY_DELAY_MS)
                                startExtractionAttempts(webView, hasExtracted, continuation)
                            }
                        } else {
                            hasExtracted.set(true)
                            continuation.resume(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing extraction result: ${e.message}")
                e.printStackTrace()
                
                if (retryCount < MAX_RETRIES) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(RETRY_DELAY_MS)
                        startExtractionAttempts(webView, hasExtracted, continuation)
                    }
                } else if (!hasExtracted.get()) {
                    hasExtracted.set(true)
                    continuation.resume(null)
                }
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
                // Parse the JSON result, removing quotes at the beginning and end
                val cleanResult = result.trim('"').replace("\\\"", "\"").replace("\\\\", "\\")
                val jsonObject = JSONObject(cleanResult)
                
                val title = jsonObject.optString("title", "")
                val artist = jsonObject.optString("artist", "")
                
                // Validate the extracted data
                if (title.isBlank()) {
                    Log.e(TAG, "❌ Extracted title is blank")
                    if (retryCount < MAX_RETRIES) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(RETRY_DELAY_MS)
                            startExtractionAttempts(webView, hasExtracted, continuation)
                        }
                        return
                    }
                }
                
                if (artist.isBlank()) {
                    Log.w(TAG, "⚠️ Extracted artist is blank, but continuing with title only")
                }
                
                Log.d(TAG, "✅ Successfully extracted - Title: \"$title\", Artist: \"$artist\"")
                
                hasExtracted.set(true)
                extractionCompleted = true
                continuation.resume(SongInfo(title = title, artist = artist))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing extraction result: ${e.message}")
                e.printStackTrace()
                
                // Try one more time for JSON parsing issues
                try {
                    // Alternative approach: just extract the title and artist directly
                    val titleMatch = "\"title\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(result)
                    val artistMatch = "\"artist\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(result)
                    
                    val title = titleMatch?.groupValues?.getOrNull(1)
                    val artist = artistMatch?.groupValues?.getOrNull(1)
                    
                    if (!title.isNullOrBlank()) {
                        Log.d(TAG, "✅ Extracted with regex - Title: \"$title\", Artist: \"$artist\"")
                        hasExtracted.set(true)
                        extractionCompleted = true
                        continuation.resume(SongInfo(title = title, artist = artist ?: ""))
                        return
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Regex extraction also failed: ${e2.message}")
                }
                
                if (retryCount < MAX_RETRIES) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(RETRY_DELAY_MS)
                        startExtractionAttempts(webView, hasExtracted, continuation)
                    }
                } else if (!hasExtracted.get()) {
                    hasExtracted.set(true)
                    continuation.resume(null)
                }
            }
        }
    }
} 