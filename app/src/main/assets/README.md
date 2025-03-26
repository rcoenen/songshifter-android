# SongShifter v1.0.0

A simple Android app that lets you open YouTube Music links in Spotify, and Spotify links in YouTube Music.

This is an open source project! Check out our [GitHub repository](https://github.com/rcoenen/songshifter-android) to see the source code, report issues, or contribute.

## What it does

- 🎵 Click a Spotify link → Opens in YouTube Music
- 🎵 Click a YouTube Music link → Opens in Spotify

That's it! No more copying and pasting song names or manually searching.

![How SongShifter works](how-it-works.gif)

## Download & Setup

This is a free, open-source app distributed as a debug build on GitHub (no app store required).

1. [Download the debug APK](https://github.com/rcoenen/songshifter-android/releases/tag/v1.0.0) from our GitHub releases
2. Allow installation from unknown sources in your Android settings
3. Install the APK
4. Choose which platform you use (YouTube Music or Spotify)
5. Start clicking those previously "unplayable" links!

Note: We distribute debug builds to keep the app free and open source, avoiding app store restrictions and paid developer accounts.

## Requirements

- Android 7.0 or newer (works best on Android 11+)
- YouTube Music or Spotify app installed

## Known Issues

⚠️ **IMPORTANT**: YouTube Music to Spotify redirection is currently broken on non-Pixel devices. The app has only been tested and verified to work on Google Pixel phones. If you have a non-Pixel device, YouTube Music links may open in your browser instead of Spotify.

Other known issues:
- YouTube Music needs to be running in the background for redirections to work
- First YouTube Music search after app launch might fail (just try again)

## TODO

Future improvements we're considering:

- Fix YouTube Music cold start issues (first search failing)
- Add Shazam link support
- Add toast message when YouTube Music needs a "warm-up" launch
- Test more edge cases with various link formats
- Collect user feedback for improvements

## Contributing

Found a bug? Have a suggestion? Feel free to:

- Open an issue
- Submit a pull request
- Test and provide feedback

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
