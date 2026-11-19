# rencana-mobile

Android and iOS app for **Rencana**, the travel & budget planner with Koper the suitcase. Same features as the web app ([`rencana-web`](https://github.com/zuramai/rencana-web)): sign-in (username or Google), plans, itinerary, other costs, exchange rates, tickets (photos and PDFs), PDF/Excel exports, settings, English and Bahasa Indonesia. Both clients talk to [`rencana-backend`](https://github.com/zuramai/rencana-backend); the server address is configurable in the app.

Stack: Kotlin Multiplatform with a shared Compose Multiplatform UI (`shared/`), Ktor client, kotlinx-serialization, navigation-compose, Coil 3, FileKit. Package / bundle id: `com.zuramai.rencana`.

## Layout

- `shared/` — all app code (`commonMain`: data, domain, i18n, UI; `androidMain` / `iosMain`: platform layer: secure storage, crypto, OAuth browser, money formatting).
- `androidApp/` — Android entry point (`MainActivity`, `OAuthRedirectActivity`, manifest, network security config).
- `iosApp/` — Xcode project; its build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.
- `i18n/mobile.{en,id}.json` — mobile-only strings; `scripts/gen-strings.mjs` merges them with the web locales into `shared/.../i18n/S.kt`.

## Prerequisites

- JDK 21 (arm64 on Apple Silicon), Android SDK with `platforms;android-37.0` (compileSdk 37, targetSdk 36, minSdk 24).
- Xcode 16+ with an iOS simulator runtime for iOS (deployment target 16.0).
- Node 24 for the strings script.

```bash
export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$(brew --prefix)/share/android-commandlinetools
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
```

## Build, run, test

```bash
./gradlew :shared:testAndroidHostTest                 # common unit tests (URL parsing, PKCE, money, periods, ApiClient)
./gradlew :androidApp:installDebug                    # Android debug build on a device/emulator
./gradlew :androidApp:assembleRelease                 # R8-minified release build
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' -derivedDataPath build/ios \
  CODE_SIGN_STYLE=Manual CODE_SIGN_IDENTITY=- DEVELOPMENT_TEAM= build   # ad-hoc signing: the Keychain needs the simulated entitlements
node scripts/gen-strings.mjs [../rencana-web/i18n/locales]   # after changing web or mobile strings
```

Debug builds default to the local backend (`http://10.0.2.2:8787` on the Android emulator, `http://localhost:8787` on the iOS simulator) and accept `http://`. Release builds accept only `https://` server addresses and have no default; the user enters the server on first launch.

## Security

- Tokens (15-minute access token, rotating refresh token) live in the Android Keystore (AES-256-GCM, StrongBox when available) or the iOS Keychain (`WhenUnlockedThisDeviceOnly`), bound to the server address they came from.
- The bearer token is only sent to the configured backend host; avatars load through a separate unauthenticated client. No disk image cache.
- Google sign-in runs in a Custom Tab / `ASWebAuthenticationSession` with PKCE and a `state` check; the backend returns a single-use code bound to the app's PKCE challenge.
- Android: no backup or device transfer of app data, cleartext traffic only in debug builds, FileProvider not exported. Exported files live in the cache and are wiped at start and on logout.

## Shared code

The domain logic (amount parsing, period and day labels, item types) is a Kotlin port of `rencana-backend/src/shared`; keep them in sync when either changes.
