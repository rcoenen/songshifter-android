# Music Redirector Android App

Music Redirector is a lightweight Android application that intercepts music streaming links and redirects them to your preferred platform.

## Features

- Intercept links from Spotify, YouTube Music, and Shazam
- Extract song title and artist information from the intercepted links
- Redirect to your preferred music platform (Spotify or YouTube Music)
- Customizable settings to control redirection behavior
- Material Design UI following modern Android development practices

## How It Works

1. When a supported music link is clicked (Spotify, YouTube Music, or Shazam), the app intercepts it
2. The app extracts the song information from the link
3. Based on your preferences, it redirects to your preferred music platform with a search query for the song
4. If extraction fails, it falls back to opening the original link

## Setup

1. Install the app
2. On first launch, the app will show you how to set it as the default handler for music links
3. Configure your preferred platform and redirection settings in the app's settings screen

## Technical Details

- Developed in Kotlin
- Uses modern Android architecture components (ViewModel, LiveData)
- Uses JSoup library for HTML parsing
- Follows Material Design guidelines

## Requirements

- Android 7.0 (API level 24) or higher

## Building from Source

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app 