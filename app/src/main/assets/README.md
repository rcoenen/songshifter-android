# SongShifter

Don't you hate it when you, as a YouTube Music user, get Spotify links sent via messaging apps from your friends and cannot click to play them? Or as a Spotify user, you might get YouTube Music links that you cannot play? Well, now we have a solution!

SongShifter seamlessly bridges the gap between music platforms by automatically redirecting links between services based on your preferences. No more frustration when friends share music from platforms you don't use - SongShifter handles the conversion for you.

## Status (v1.5.3-b357)

### Working Features
- ✅ Spotify to YouTube Music redirection (fully tested and stable)
- ✅ YouTube Music to Spotify redirection (fully tested and stable)
- ✅ Shazam link interception (implemented but needs more testing)
- ✅ Clean and intuitive UI with platform selection
- ✅ Warning banner when YouTube Music needs to be running
- ✅ Test functionality for verifying redirections
- ✅ Dedicated settings buttons for easier configuration
- ✅ Accurate link handling status detection on Android 11+
- ✅ Platform-aware YouTube Music status detection (Pixel vs. non-Pixel devices)

### Platform-Specific Features
- ✅ Pixel phones: Detects YouTube Music as system app and guides users to disable it
- ✅ Non-Pixel phones: Detects YouTube Music as regular app and guides users to install it

### Limited Testing
- ⚠️ Shazam link processing and redirection
- ⚠️ Edge cases with various link formats

### Known Requirements
- **Android 11 (API 30) or higher recommended** for proper domain verification handling
- Android 7.0 (API 24) minimum required for basic functionality
- YouTube Music must be running in the background for YouTube Music redirections to work
- **YouTube Music requires an initial cold start/warm-up** - first search may fail, requiring a second attempt
- App needs permission to handle links (can be configured in system settings)
- Tested on Pixel 6a with Android 15 (development target)

## System Compatibility
| Android Version | Compatibility | Notes |
|-----------------|---------------|-------|
| Android 11+ (API 30+) | ✅ Full | All features work including accurate domain verification status |
| Android 8-10 (API 26-29) | ⚠️ Partial | Basic functionality works, but domain verification status may be inaccurate |
| Android 7 (API 24-25) | ⚠️ Limited | Core functionality only, UI may have inconsistencies |
| Android 6 or lower | ❌ Not Supported | App will not function correctly |

## Usage

1. Select your preferred platform (YouTube Music or Spotify)
2. Enable link handling in system settings (tap the "Open App Link Settings" button)
3. Share music links from supported platforms to test redirection

## Development Status

Current build: 357
Version: 1.5.3

### Recent Updates
- Added platform-aware YouTube Music detection to properly handle both Pixel and non-Pixel devices
- Fixed step ordering in setup UI to ensure logical 1-2-3 sequence
- Fixed visual ordering of platform status items to match step numbers
- Improved circular redirection detection and prevention when using Spotify to YouTube Music mode
- Enhanced YouTube Music detection to properly handle disabled apps
- Added fallback methods for YouTube Music launching when URI schemes aren't supported
- Automated configuration of link handling settings based on the selected mode
- Improved error messages with clear guidance for users
- Fixed several UI inconsistencies and polished the overall user experience

### Next Steps
- Enhance detection of YouTube Music cold start state with user guidance
- Add toast message to inform users when YouTube Music needs a "warm-up" launch
- Additional testing of Shazam link handling
- Edge case testing with various link formats
- User feedback collection and bug fixes

## Changelog

### v1.5.3-b357 (Build 357) - March 26, 2024
- Added platform-aware YouTube Music status detection that properly handles:
  - Pixel phones (where YouTube Music is a system app that needs to be disabled)
  - Non-Pixel phones (where YouTube Music needs to be installed)
- Fixed visual ordering of setup steps to ensure correct 1-2-3 sequence
- Fixed view ordering in UI to match step numbers
- Improved user guidance with platform-specific instructions

### v1.5.2-b356 (Build 356) - March 26, 2024
- Fixed step ordering in Spotify→YouTube Music mode to show steps in correct 1-2-3 sequence
- Updated step numbers in status text for consistency

### v1.5.1-b353 (Build 353) - March 26, 2024
- Added platform detection architecture to handle different device types
- Implemented SystemYouTubeMusicStatus for Pixel devices
- Implemented RegularYouTubeMusicStatus for other Android phones

### v1.5.0-b349 (Build 349) - March 26, 2024
- Renamed app from MusicRedirector to SongShifter
- Updated all package references and branding
- Added metadata for improved link handling
- Fixed various UI references to reflect new app name

### v1.5.0-b292 (Build 292) - March 25, 2024
- Fixed step ordering in setup UI to ensure logical 1-2-3 progression
- Added automatic detection and prevention of circular redirections
- Implemented fallback methods for YouTube Music launching
- Fixed YouTube Music detection to correctly identify enabled vs. installed state
- Improved error handling with more informative messages
- Added vibration feedback for better user interaction
- Enhanced Spotify uninstall process with direct app info page opening

### v1.4.0-b258 (Build 258) - March 25, 2024
- Streamlined UI with directly clickable status indicators for faster fixes
- Removed redundant warning messages and improved visual hierarchy
- Added "tap to fix" guidance for clearer user interaction
- Integrated Configuration Incomplete status within Setup Status section
- Fixed layout issues to ensure key elements are visible without scrolling

### v1.3.0-b223 (Build 223) - March 24, 2024
- Fixed domain verification detection and UI display to properly handle SELECTED domains
- Added dedicated settings buttons and removed clickable status indicators
- Improved error handling for link verification status
- Fixed discrepancy between UI status and actual Android settings

### v1.2.0-b190 (Build 190) - March 24, 2024
- Enhanced YouTube Music integration with app state detection
- Improved link handling with better error reporting
- UI refinements and stability improvements

### v1.1.0-b145 (Build 145) - March 22-23, 2024
- Stable Spotify to YouTube Music redirection with improved UI
- Added Shazam integration (needs further testing)
- Implemented YouTube Music to Spotify redirection
- Cleaner UI with proper warning banners and test functionality

### v1.1-b130 (Build 130) - March 22, 2024
- Added YouTube Music to Spotify redirection with deep linking support
- Improved error handling and user feedback
- Enhanced UI for better user experience

### v1.0.8-b100 (Build 100) - March 21-22, 2024
- First working version that successfully redirects Spotify links to YouTube Music
- Basic UI with functional redirections
- Core functionality established

### v1.0.0-b80 (Build 80) - March 21, 2024
- First stable version using Spotify embed page extraction
- Initial implementation of link handling
- Basic UI and navigation

### v1.0-b50 (Build 50) - March 21, 2024
- First working version of SongShifter with Material Design 3 UI
- Spotify to YouTube Music redirection prototype
- Basic functionality and concept demonstration

## Contributing

Please report any issues or unexpected behavior when:
- Using YouTube Music to Spotify redirection
- Sharing Shazam links
- Using different link formats

##  MIT License

Copyright (c) 2024 SongShifter

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial 
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT 
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
