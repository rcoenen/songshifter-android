package com.songshifter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.songshifter.utils.ReadmeLoader
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension

/**
 * Activity to display the full README.md contents in a WebView
 */
class ReadmeActivity : AppCompatActivity() {
    private val TAG = "ReadmeActivity"
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var parser: Parser
    private lateinit var renderer: HtmlRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_readme)

        // Initialize CommonMark parser and renderer with extensions
        val extensions = listOf(
            TablesExtension.create(),
            StrikethroughExtension.create()
        )
        parser = Parser.builder()
            .extensions(extensions)
            .build()
        renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .softbreak("<br>")
            .build()

        // Initialize views
        webView = findViewById(R.id.readmeWebView)
        progressBar = findViewById(R.id.progressBar)

        // Enable JavaScript and content features for better rendering
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            loadsImagesAutomatically = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        // Set up WebViewClient to handle external links
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    // Open links in external browser
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it))
                    startActivity(intent)
                }
                return true
            }
        }

        // Set up close button
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            finish()
        }

        // Load README from GitHub
        loadReadmeFromGitHub()
    }
    
    /**
     * Loads the README.md file directly from GitHub
     */
    private fun loadReadmeFromGitHub() {
        progressBar.visibility = View.VISIBLE
        webView.visibility = View.GONE

        lifecycleScope.launch {
            ReadmeLoader.loadReadme()
                .onSuccess { readmeContent ->
                    Log.d(TAG, "Raw README content: $readmeContent")
                    // Convert markdown to HTML and display
                    val htmlContent = markdownToHtml(readmeContent)
                    Log.d(TAG, "Converted HTML content: $htmlContent")
                    webView.loadDataWithBaseURL(
                        "https://raw.githubusercontent.com/rcoenen/songshifter-android/master/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    progressBar.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                }
                .onFailure { error ->
                    Log.e(TAG, "Error loading README.md: ${error.message}")
                    // Display error message if README.md cannot be loaded
                    val errorHtml = """
                        <html>
                        <body style="background-color: #f5f5f5; padding: 16px;">
                            <h2 style="color: #B3261E;">Error Loading Content</h2>
                            <p>Could not load README.md: ${error.message}</p>
                            <p>Please check your internet connection and try again.</p>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                    progressBar.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                }
        }
    }
    
    /**
     * Converts markdown content to HTML for WebView rendering
     */
    private fun markdownToHtml(markdown: String): String {
        // Parse markdown to AST and render to HTML
        val document = parser.parse(markdown)
        val htmlContent = renderer.render(document)
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        padding: 16px;
                        background-color: #f5f5f5;
                        margin: 0;
                        font-size: 16px;
                    }
                    h1 {
                        color: #4B00E0;
                        font-size: 24px;
                        margin-top: 0;
                        margin-bottom: 16px;
                        padding-bottom: 8px;
                        border-bottom: 1px solid #eee;
                    }
                    h2 {
                        color: #4B00E0;
                        font-size: 20px;
                        margin-top: 24px;
                        margin-bottom: 12px;
                        padding-bottom: 6px;
                        border-bottom: 1px solid #eee;
                    }
                    p {
                        margin: 16px 0;
                        line-height: 1.6;
                    }
                    ul, ol {
                        padding-left: 24px;
                        margin: 16px 0;
                    }
                    li {
                        margin: 8px 0;
                        line-height: 1.4;
                    }
                    a {
                        color: #4B00E0;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    code {
                        background-color: #f0f0f0;
                        padding: 2px 4px;
                        border-radius: 4px;
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 14px;
                    }
                    pre {
                        background-color: #f0f0f0;
                        padding: 16px;
                        border-radius: 4px;
                        overflow-x: auto;
                        margin: 16px 0;
                    }
                    pre code {
                        background-color: transparent;
                        padding: 0;
                        font-size: 14px;
                        line-height: 1.4;
                    }
                    blockquote {
                        margin: 16px 0;
                        padding-left: 16px;
                        border-left: 4px solid #4B00E0;
                        color: #666;
                    }
                    hr {
                        border: none;
                        border-top: 1px solid #eee;
                        margin: 24px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 16px 0;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: #f5f5f5;
                    }
                    tr:nth-child(even) {
                        background-color: #f9f9f9;
                    }
                </style>
            </head>
            <body>
                $htmlContent
            </body>
            </html>
        """.trimIndent()
    }
} 