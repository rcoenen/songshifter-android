package com.songshifter

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Activity to display the full README.md contents in a WebView
 */
class ChangelogActivity : AppCompatActivity() {
    private val TAG = "ChangelogActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        // Set up close button
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            finish()
        }

        // Load README.md into WebView
        loadReadmeIntoWebView()
    }
    
    /**
     * Loads the README.md file from assets into a WebView with proper rendering
     */
    private fun loadReadmeIntoWebView() {
        try {
            // Read the README.md file from assets
            val inputStream = assets.open("README.md")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val readmeContent = reader.readText()
            
            // Convert markdown to HTML
            val htmlContent = markdownToHtml(readmeContent)
            
            // Load HTML into WebView
            val webView = findViewById<WebView>(R.id.readmeWebView)
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading README.md: ${e.message}")
            // Display error message if README.md cannot be loaded
            val webView = findViewById<WebView>(R.id.readmeWebView)
            webView.loadData(
                "<html><body><h2>Error Loading Content</h2><p>Could not load README.md: ${e.message}</p></body></html>",
                "text/html",
                "UTF-8"
            )
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
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: #4B00E0;
                    color: white;
                    font-weight: bold;
                }
                tr:nth-child(even) {
                    background-color: #f9f9f9;
                }
                tr:hover {
                    background-color: #f1f1f1;
                }
            </style>
        """.trimIndent()
        
        // Process table syntax first
        var processedMarkdown = markdown
        val tablePattern = Regex("\\|.*\\|\\n\\|[-:\\s|]*\\|(?:\\n\\|.*\\|)+", RegexOption.MULTILINE)
        val tableMatcher = tablePattern.findAll(processedMarkdown)
        
        for (tableMatch in tableMatcher) {
            val tableContent = tableMatch.value
            val tableRows = tableContent.split("\n")
            
            if (tableRows.size >= 2) {
                val headerRow = tableRows[0]
                
                // Skip the separator row (|---|---|)
                var htmlTable = "<table>\n<thead>\n<tr>\n"
                
                // Process header cells
                val headerCells = headerRow.trim('|').split("|")
                for (cell in headerCells) {
                    htmlTable += "<th>${cell.trim()}</th>\n"
                }
                
                htmlTable += "</tr>\n</thead>\n<tbody>\n"
                
                // Process data rows
                for (i in 2 until tableRows.size) {
                    val row = tableRows[i]
                    if (row.isBlank() || !row.contains("|")) continue
                    
                    htmlTable += "<tr>\n"
                    val cells = row.trim('|').split("|")
                    
                    for (cell in cells) {
                        htmlTable += "<td>${cell.trim()}</td>\n"
                    }
                    
                    htmlTable += "</tr>\n"
                }
                
                htmlTable += "</tbody>\n</table>"
                
                // Replace the markdown table with the HTML table
                processedMarkdown = processedMarkdown.replace(tableContent, htmlTable)
            }
        }
        
        // Simple markdown to HTML conversion
        var html = processedMarkdown
            // Headers
            .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
            .replace(Regex("^#### (.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
            
            // Bold and italic
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
            
            // Lists
            .replace(Regex("^- (.+)$", RegexOption.MULTILINE), "<li>$1</li>")
            .replace(Regex("(<li>.+</li>\\n)+"), "<ul>$0</ul>")
            
            // Code blocks - condensed styling with reduced line height
            .replace(Regex("```([\\s\\S]*?)```")) { matchResult ->
                val codeContent = matchResult.groupValues[1]
                    .trim()
                    .replace("\n", "<br>") // Replace newlines with <br> tags
                "<pre><code style=\"line-height:1.2; display:block; white-space:pre-wrap;\">$codeContent</code></pre>"
            }
            
            // Inline code
            .replace(Regex("`([^`]+)`"), "<code>$1</code>")
            
            // Links
            .replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "<a href=\"$2\">$1</a>")
            
            // Paragraphs (apply last)
            .replace(Regex("^([^<].+)$", RegexOption.MULTILINE), "<p>$1</p>")
        
        // Complete HTML document
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                $css
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()
    }
} 