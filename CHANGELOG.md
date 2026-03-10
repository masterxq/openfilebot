# Changelog

## 0.9.4

- Split portable artifacts by platform / architecture and standardized naming:
- Linux `aarch64`: `*-portable-linux-aarch64.tar.gz`
- Linux `x86_64`: `*-portable-linux-x86_64.tar.gz`
- Windows `x64`: `*-portable-win64.zip`
- Updated CI and manual release workflows to build, sign, and publish the new portable artifact set (`.tar.gz` / `.zip`).
- Added a manual GitHub Actions workflow to refresh Linux native libraries and open an automated pull request (`refresh-native-libs.yml`).
- Refreshed Linux native library handling: removed obsolete `armv7l` portable payload, added `libzen.so.0 -> libzen.so` SONAME symlinks, included `lib7-Zip-JBinding.so` for `linux-armv8`, and bundled `fpcalc` for both portable Linux architectures.
- Updated portable update signature trust key and maintainer public key material used by installer scripts.

## 0.9.3

- Migrated the codebase namespace from `net.filebot` to `org.openfilebot` and cleaned up launcher/resource wiring across build and installer configurations.
- Prepared the next release by embedding media resources, improving matching behavior, and simplifying / cleaning up web API integrations.
- Updated rename bindings and expanded automated tests to improve compatibility and reduce regressions.
- Ongoing local work includes additional UI and web-service refinements for the next release.
- Fixed rename history loading on modern Java runtimes by bundling the required JAXB runtime classes into the executable package.
- Changed default user data location for OpenFileBot to `~/.openfilebot` (instead of sharing `~/.filebot`).
- Added one-time history migration: if OpenFileBot has no own `history.xml`, it imports legacy FileBot history from `~/.filebot/history.xml`.
- Added safe migration validation: malformed legacy history files are detected and skipped.

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
