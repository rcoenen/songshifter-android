package com.songshifter

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.songshifter.utils.ReadmeLoader
import kotlinx.coroutines.launch

/**
 * Activity to display the full README.md contents in a WebView
 */
class ChangelogActivity : AppCompatActivity() {
    private val TAG = "ChangelogActivity"
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        // Initialize views
        webView = findViewById(R.id.readmeWebView)
        progressBar = findViewById(R.id.progressBar)

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
                    // Convert markdown to HTML and display
                    val htmlContent = markdownToHtml(readmeContent)
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
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
        // Basic stylesheet for better readability
        val css = """
            <style>
                body {
                    font-family: 'Roboto', sans-serif;
                    line-height: 1.6;
                    color: #333;
                    padding: 16px;
                    background-color: #f5f5f5;
                }
                h1, h2 {
                    color: #4B00E0;
                }
                h3, h4 {
                    color: #5D3FD3;
                }
                code {
                    background-color: #f0f0f0;
                    padding: 2px 4px;
                    border-radius: 4px;
                    font-family: monospace;
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
                    line-height: 1.2;
                    white-space: pre-wrap;
                    font-family: monospace;
                }
                pre br {
                    line-height: 1.2;
                    margin-bottom: 0;
                }
                a {
                    color: #4B00E0;
                }
                ul, ol {
                    padding-left: 20px;
                }
                li {
                    margin-bottom: 8px;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                    overflow-x: auto;
                    display: block;
                }
                th, td {
                    border: 1px solid #ddd;
                    padding: 8px;
                    text-align: left;
                }
                th {
                    background-color: #4B00E0;
                    color: white;
                }
                tr:nth-child(even) {
                    background-color: #f9f9f9;
                }
            </style>
            <div class="markdown-body">
                $markdown
            </div>
        """
        return css
    }
} 