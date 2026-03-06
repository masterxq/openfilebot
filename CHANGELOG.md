# Changelog

## 0.9.2

- Better matching for series and multi-episode files, including tricky naming patterns.
- Improved media metadata handling (e.g. HDR detection) and added better naming helpers (including Kodi-style naming).
- More reliable local data handling: important matching lists are now bundled directly with the app instead of downloaded at runtime.
- Improved stability for online lookups with safer fallback behavior when external services fail or timeout.
- Removed legacy TheTVDB v1 code and cleaned up old API-related parts.
- Updated core dependencies and packaging to improve compatibility on current Linux/Java setups.
- Expanded and hardened tests to reduce regressions.
- Fixed build tests and added new ones for better coverage of media detection and matching logic.

## 0.9.1

- removed all donation and pay functions.
- removed getting started guide
- replaced logo
- Pipeline Signing
- Updated update mechanism to use GitHub releases
- Added debian release
- Working on .msi installer (Never tested! I have no Windows machine)
- Updated state text
- Added more tests
- removed unnecessary code, files and dependencies
- Updated README and other documentation
- Added more tests for better media detection, but still failing.
- Updated to recent java version: Java 21-24 should work, build with Java 21.
