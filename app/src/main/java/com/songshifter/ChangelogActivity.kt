package com.songshifter

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Activity to display the changelog information from README.md
 */
class ChangelogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        // Set up close button
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            finish()
        }

        // Load and parse changelog from README.md in assets
        loadChangelogFromReadme()
    }
    
    /**
     * Loads and parses the changelog section from README.md
     */
    private fun loadChangelogFromReadme() {
        val currentVersionText = findViewById<TextView>(R.id.currentVersionChanges)
        val versionNumberText = findViewById<TextView>(R.id.versionNumber)
        val fullChangelogText = findViewById<TextView>(R.id.fullChangelog)
        
        try {
            // Read the README.md file from assets
            val inputStream = assets.open("README.md")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val readmeContent = reader.readText()
            
            // Extract the changelog section
            val changelogSection = extractChangelogSection(readmeContent)
            
            // Extract the current version information
            val (currentVersionHeader, currentVersionContent) = extractCurrentVersion(changelogSection)
            
            // Extract previous versions
            val previousVersionsContent = extractPreviousVersions(changelogSection, currentVersionHeader)
            
            // Update UI
            versionNumberText.text = currentVersionHeader.replace("###", "").trim()
            currentVersionText.text = formatChangelogContent(currentVersionContent)
            fullChangelogText.text = formatChangelogContent(previousVersionsContent)
            
        } catch (e: Exception) {
            // If there's an error, display a fallback message
            currentVersionText.text = "Could not load changelog. Please check the GitHub repository for the latest changes."
            fullChangelogText.text = "Error: ${e.message}"
        }
    }
    
    /**
     * Extracts the changelog section from the README content
     */
    private fun extractChangelogSection(readmeContent: String): String {
        val changelogSectionStart = readmeContent.indexOf("## Changelog")
        if (changelogSectionStart == -1) return ""
        
        val nextSectionStart = readmeContent.indexOf("##", changelogSectionStart + 1)
        
        return if (nextSectionStart == -1) {
            readmeContent.substring(changelogSectionStart)
        } else {
            readmeContent.substring(changelogSectionStart, nextSectionStart)
        }
    }
    
    /**
     * Extracts the current version header and content
     */
    private fun extractCurrentVersion(changelogSection: String): Pair<String, String> {
        val versionPattern = Regex("###\\s+v[\\d.]+.*?\\(Build\\s+\\d+\\).*?\\n")
        val versionMatcher = versionPattern.find(changelogSection)
        
        if (versionMatcher != null) {
            val versionHeader = versionMatcher.value.trim()
            val nextVersionIndex = changelogSection.indexOf("###", versionMatcher.range.last)
            
            val versionContent = if (nextVersionIndex == -1) {
                changelogSection.substring(versionMatcher.range.last)
            } else {
                changelogSection.substring(versionMatcher.range.last, nextVersionIndex)
            }
            
            return Pair(versionHeader, versionContent)
        }
        
        return Pair("Current Version", "No version information available.")
    }
    
    /**
     * Extracts all previous versions content
     */
    private fun extractPreviousVersions(changelogSection: String, currentVersionHeader: String): String {
        val versionHeaderIndex = changelogSection.indexOf(currentVersionHeader)
        if (versionHeaderIndex == -1) return ""
        
        val nextVersionHeaderIndex = changelogSection.indexOf("###", versionHeaderIndex + currentVersionHeader.length)
        if (nextVersionHeaderIndex == -1) return ""
        
        return changelogSection.substring(nextVersionHeaderIndex)
    }
    
    /**
     * Formats the markdown changelog content for display
     */
    private fun formatChangelogContent(content: String): String {
        var formatted = content.trim()
            .replace(Regex("###\\s+"), "") // Remove heading markers
            .replace(Regex("-\\s+"), "• ") // Replace bullet points with bullets
        
        return formatted
    }
} 