package com.musicredirector

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebViewExtractor(private val context: Context) {
    private val TAG = "WebViewExtractor"

    suspend fun extractFromSpotify(url: String): SongInfo? {
        Log.d(TAG, "Starting WebView extraction for URL: $url")
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d(TAG, "WebView page finished loading")
                            
                            // Inject JavaScript to extract song info
                            val script = """
                                (function() {
                                    try {
                                        // Try to get from meta tags first
                                        const titleMeta = document.querySelector('meta[property="og:title"]');
                                        const artistMeta = document.querySelector('meta[property="og:description"]');
                                        
                                        if (titleMeta && titleMeta.content) {
                                            const title = titleMeta.content;
                                            const artist = artistMeta ? artistMeta.content : '';
                                            return { title, artist };
                                        }
                                        
                                        // Fallback to page title
                                        const title = document.title;
                                        return { title, artist: '' };
                                    } catch (e) {
                                        console.error('Error in extraction script:', e);
                                        return null;
                                    }
                                })();
                            """.trimIndent()
                            
                            Log.d(TAG, "Injecting JavaScript for extraction")
                            evaluateJavascript(script) { result ->
                                Log.d(TAG, "JavaScript evaluation result: $result")
                                
                                try {
                                    if (result != "null") {
                                        // Parse the result
                                        val title = result.substringAfter("\"title\":\"").substringBefore("\"")
                                        val artist = result.substringAfter("\"artist\":\"").substringBefore("\"")
                                        
                                        Log.d(TAG, "Extracted from WebView - Title: \"$title\", Artist: \"$artist\"")
                                        continuation.resume(SongInfo(title, artist))
                                    } else {
                                        Log.e(TAG, "JavaScript extraction returned null")
                                        continuation.resume(null)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing JavaScript result: ${e.message}")
                                    e.printStackTrace()
                                    continuation.resume(null)
                                }
                            }
                        }
                        
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Log.e(TAG, "WebView error: $errorCode - $description")
                            continuation.resumeWithException(Exception("WebView error: $errorCode - $description"))
                        }
                    }
                }
                
                Log.d(TAG, "Loading URL in WebView")
                webView.loadUrl(url)
                
                continuation.invokeOnCancellation {
                    Log.d(TAG, "WebView extraction cancelled")
                    webView.destroy()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in WebView extraction: ${e.message}")
                e.printStackTrace()
                continuation.resumeWithException(e)
            }
        }
    }
} 