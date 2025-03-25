# Binus Music Redirector

Android app that redirects music links between platforms based on your preference.

## Status (v1.4.0-b258)

### Working Features
- ✅ Spotify to YouTube Music redirection (fully tested and stable)
- ✅ YouTube Music to Spotify redirection (fully tested and stable)
- ✅ Shazam link interception (implemented but needs more testing)
- ✅ Clean and intuitive UI with platform selection
- ✅ Warning banner when YouTube Music needs to be running
- ✅ Test functionality for verifying redirections
- ✅ Dedicated settings buttons for easier configuration
- ✅ Accurate link handling status detection on Android 11+

### Limited Testing
- ⚠️ Shazam link processing and redirection
- ⚠️ Edge cases with various link formats

### Known Requirements
- **Android 11 (API 30) or higher recommended** for proper domain verification handling
- Android 7.0 (API 24) minimum required for basic functionality
- YouTube Music must be running in the background for YouTube Music redirections to work
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

Current build: 258
Version: 1.4.0

### Recent Updates
- Improved UI with cleaner status indicators and better user interaction
- Made error states directly actionable with "tap to fix" functionality
- Removed redundant warning messages and improved visual hierarchy
- Relocated the Configuration Incomplete status into the Setup Status card
- Refactored extractor architecture with dedicated classes for each music platform
- Improved domain verification detection to properly handle Android 11+ SELECTED domains
- Added dedicated settings buttons and removed clickable status indicators
- Fixed UI status display to accurately reflect Android link handling settings
- Robust song information extraction using platform-specific methods

### Next Steps
- Additional testing of Shazam link handling
- Edge case testing with various link formats
- User feedback collection and bug fixes

## Changelog

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
- First working version of Binus with Material Design 3 UI
- Spotify to YouTube Music redirection prototype
- Basic functionality and concept demonstration

## Contributing

Please report any issues or unexpected behavior when:
- Using YouTube Music to Spotify redirection
- Sharing Shazam links
- Using different link formats

## License

[Insert License Information] 