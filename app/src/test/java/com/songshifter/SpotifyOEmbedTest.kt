package com.songshifter

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.serialization.json.*

class SpotifyOEmbedTest {
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    companion object {
        // Test set of real Spotify track URLs and their expected responses
        private val TEST_CASES = listOf(
            TestCase(
                "2OoS1aZFAFscH1cUiKlazH",
                "Refuse/Resist - Sepultura",
                "Refuse/Resist",
                "Sepultura"
            ),
            TestCase(
                "11dFghVXANMlKmJXsNCbNl",
                "Shape of You - Ed Sheeran",
                "Shape of You",
                "Ed Sheeran"
            )
            // Add more test cases as needed
        )
    }

    data class TestCase(
        val trackId: String,
        val fullTitle: String,
        val expectedSongTitle: String,
        val expectedArtist: String
    )

    data class OEmbedResult(
        val responseCode: Int,
        val title: String?,
        val songTitle: String?,
        val artist: String?,
        val error: String?
    )

    @Test
    fun testOEmbedEndpoint() {
        var passCount = 0
        var failCount = 0

        println("\n=== Spotify oEmbed Test Results ===\n")

        TEST_CASES.forEach { testCase ->
            print("Testing track ${testCase.trackId}: ")

            // Queue mock response
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "title": "${testCase.fullTitle}",
                        "type": "rich",
                        "version": "1.0"
                    }
                """.trimIndent())
            mockWebServer.enqueue(mockResponse)

            val result = testSingleUrl("https://open.spotify.com/track/${testCase.trackId}")

            when {
                result.responseCode != 200 -> {
                    println("❌ FAIL - HTTP ${result.responseCode}")
                    println("   Error: ${result.error}")
                    failCount++
                }
                result.title == null -> {
                    println("❌ FAIL - No title in response")
                    println("   Error: ${result.error}")
                    failCount++
                }
                !result.title.contains(" - ") -> {
                    println("❌ FAIL - Title doesn't contain artist separator")
                    println("   Title: ${result.title}")
                    failCount++
                }
                result.songTitle != testCase.expectedSongTitle || result.artist != testCase.expectedArtist -> {
                    println("❌ FAIL - Incorrect song or artist extraction")
                    println("   Expected: ${testCase.expectedSongTitle} - ${testCase.expectedArtist}")
                    println("   Got: ${result.songTitle} - ${result.artist}")
                    failCount++
                }
                else -> {
                    println("✅ PASS")
                    println("   Song: ${result.songTitle}")
                    println("   Artist: ${result.artist}")
                    passCount++
                }
            }
            println()
        }

        println("=== Summary ===")
        println("Total tests: ${TEST_CASES.size}")
        println("Passed: $passCount")
        println("Failed: $failCount")
        println("Success rate: ${(passCount.toFloat() / TEST_CASES.size * 100).toInt()}%")

        // Assert overall test success
        assertTrue("Some oEmbed tests failed", failCount == 0)
    }

    private fun testSingleUrl(url: String): OEmbedResult {
        try {
            val oembedUrl = mockWebServer.url("/oembed").toString() + "?url=$url"
            val connection = URL(oembedUrl).openConnection() as HttpURLConnection

            connection.apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "SongShifter Test Runner")
            }

            val responseCode = connection.responseCode

            if (responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                return OEmbedResult(responseCode, null, null, null, error)
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = Json.parseToJsonElement(response).jsonObject
            val title = json["title"]?.jsonPrimitive?.content

            if (title.isNullOrBlank()) {
                return OEmbedResult(responseCode, null, null, null, "Empty title in response")
            }

            val parts = title.split(" - ")
            if (parts.size != 2) {
                return OEmbedResult(responseCode, title, null, null, "Title doesn't contain artist separator")
            }

            val (songTitle, artist) = parts
            return OEmbedResult(responseCode, title, songTitle.trim(), artist.trim(), null)

        } catch (e: Exception) {
            return OEmbedResult(0, null, null, null, "Exception: ${e.message}")
        }
    }
} 