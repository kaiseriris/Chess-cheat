# ChessAnalyzer – building the APK

The zip root is the Gradle project root (`gradlew`, `settings.gradle`, `app/`).

Build settings for any online builder:
- Build command: `./gradlew assembleDebug`
- Output: `app/build/outputs/apk/debug/app-debug.apk` (auto-signed with the debug key, installable directly)
- JDK 17, Android SDK platform 34, Gradle 8.5 (pinned by the wrapper), Android Gradle Plugin 8.2.2
- minSdk 26, targetSdk 34

GitHub route: upload the contents of this zip to a new GitHub repo, open Actions -> "Build APK" -> Run workflow,
then download the `ChessAnalyzer-debug-apk` artifact.

Overlay: tap "Overlay" in the app, grant "Display over other apps", tap again.
