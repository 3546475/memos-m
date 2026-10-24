<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="128" alt="MemosM app icon" />

# MemosM

A native Android client for [Memos](https://usememos.com/), built with Jetpack Compose and drawn in the Miuix (HyperOS) design language.

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/yamada-sexta/memos-m/canary-build.yml?style=flat-square)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/yamada-sexta/memos-m/nightly-test.yml?style=flat-square&label=nightly%20API%20tests)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/yamada-sexta/memos-m/total?style=flat-square)
![GitHub License](https://img.shields.io/github/license/yamada-sexta/memos-m?style=flat-square)
![GitHub Repo stars](https://img.shields.io/github/stars/yamada-sexta/memos-m?style=flat-square)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/yamada-sexta/memos-m?style=flat-square)

</div>

---

**MemosM** is an Android client for [Memos](https://usememos.com/) that follows the current server releases. It supports Memos v0.26 and newer: the app probes the instance, binds the matching API adapter and falls back to the newest one for newer or unknown servers — see [Server support](#server-support). Android 7.0 (API 24) and newer are supported.

This branch ships the **Miuix-only** build: one design language in the HyperOS idiom, from the grouped settings cards and the floating capsule bottom bar to the frosted surfaces and the app's own dot-matrix loader. `main` additionally carries a Material 3 path and a style switch; everything else is kept in sync between the two. See [Branches](#branches).

The app aims at a native Android feel, fast syncing and full support for current Memos features, with an offline-first data layer and a structured filter system.

## Screenshots

<img height="300"  alt="MemosM screenshot" src="https://github.com/user-attachments/assets/aba051d0-0c6d-4d0a-9bd0-9b7d0389d000" />

<img height="300"  alt="MemosM screenshot" src="https://github.com/user-attachments/assets/c1c3ac79-1d17-402a-b538-1eafe6b624e8" />

## Why MemosM?

- **Native Android:** built for Android and its tablets rather than being a cross-platform port — bottom bar or navigation rail, home-screen widgets and share targets are part of the app, not an afterthought.
- **Efficient syncing:** paged fetching and a local cache instead of re-syncing the whole history on every refresh.
- **Offline-first by default:** a local-first cache, queued offline writes, conflict handling and optional pre-download keep the app usable without a connection.
- **Filtering that fits:** saved views, a collapsible tag tree, date windows, content types and ordering — compiled to the server's CEL filter and mirrored locally for offline use.
- **Current Memos:** tracking recent server releases buys modern Markdown, reactions and SSO instead of carrying the weight of old-version compatibility.

## Features

- **Accounts & sign-in**
  - Token, password or SSO login
  - Multiple accounts with fast switching; caches, filters and queued writes are per account
  - Local network access handling for servers on Android 17 and later
- **Memos**
  - Compose, edit, delete, pin, archive and unarchive memos
  - Share-sheet target for text, images and files
  - Comments, reactions (from the server's emoji list), linked memos and location
  - Search over content, tags and date ranges, with ordering
  - Structured filter panel: saved views, collapsible tag tree, date presets and custom ranges, content types (links/code/todos) and ordering. Applied filters persist per account and stay visible as removable tokens under the top bar
- **Markdown**
  - GFM rendering: headings, lists, task lists, tables, quotes, links and images
  - Code blocks with syntax highlighting, optional line numbers and line wrapping
  - LaTeX math, link previews, paragraph indent and CJK spacing options
- **Media**
  - Full-screen image viewer with zoom; avatar cropping
  - Video playback (ExoPlayer) and audio recording/playback
  - Attachments browser, streamed downloads and a size-limited offline media cache
- **Organize**
  - Tag tree in the profile, memo kinds (links, code, todos), archived memos, statistics and an activity calendar
  - Notifications list (no background polling)
- **Offline & sync**
  - Local-first cache with offline reads for the feed, search, map and attachments
  - Offline writes queued and replayed on reconnect, with a conflict-resolution dialog
  - Optional pre-download of text and attachments (Wi-Fi only), cache size limits
  - Sync status panel, audit log, failed-upload retry/discard
- **Appearance**
  - Miuix (HyperOS) styling; light, dark or follow-system
  - Follow-system (Material You), Memos, monochrome or custom accent colors; font and heading-size options
  - Edge-to-edge or floating capsule bottom bar, hide-on-scroll, frosted bar, reorderable items; navigation rail on tablets
  - Edit gesture for memo cards (none, single tap, double tap or long press)
- **Widgets & shortcuts**
  - Draft widget and a user-stats widget with its own configuration screen
  - Four launcher shortcuts: new memo, drafts, notifications and search
- **Backup & restore**
  - `.mmbackup` export with MessagePack + gzip and optional password protection, covering accounts, settings, caches, media and drafts — see [the backup notes](docs/backup.md)
- **Map**
  - Native map with Memos / Explore / All scopes, location pins, and composing at a chosen place
  - OpenFreeMap basemaps with an OpenStreetMap raster fallback
- **Languages**
  - English, German, Japanese, Korean, Polish, Simplified Chinese and Traditional Chinese

The Map destination appears only after the connected instance accepts the `has_location` memo filter. Older and unknown instances do not show it. Previously verified accounts can browse cached locations offline using available cached tiles. Saved views require a connection; map regions are not downloaded in advance.

## Server support

The instance probe reads the server version and binds the matching adapter:

| Server version | Adapter |
| --- | --- |
| `0.26.x` | v0.26.0 |
| `0.27.x` | v0.27.0 |
| `0.28.x`, `0.29.x` | v0.28.0 |
| `0.30.x`, newer or unknown | v0.30.0 (newest implemented) |

The probe runs against the current v0.35.3 endpoint and falls back to the unversioned `/api` endpoint, so a server that hides its version still connects.

## SSO login

Select **SSO**, enter your Memos server URL, and choose **Find providers**. Select a provider to authenticate in your browser. MemosM then creates a personal access token for the account, just as password login does. Password and manual token login remain available.

An administrator must add the app callback URI to the allowed redirect URIs for the OAuth2 client configured in Memos. Keep the existing Memos web callback registered as well.

| Build | Callback URI |
| --- | --- |
| Release | `org.example.memosm://oauth/callback` |
| Insider | `org.example.memosm.insider://oauth/callback` |
| Canary | `org.example.memosm.canary://oauth/callback` |
| Debug | `org.example.memosm.debug://oauth/callback` |

The provider must accept custom URI schemes and S256 PKCE. Providers requiring an HTTPS callback can use manual token login instead. SSO is supported by the v0.26+ adapters and the newer Memos SSO format. Registration and identity provisioning follow the server's settings. Sign-in attempts expire after ten minutes; if Android terminates the app during authentication, start sign-in again.

## Downloads

### Stable build

> MemosM is still under active development, so there is no stable build for now.

### Insider build

The Insider build is for users who want relatively new features without constant instability. While generally usable, it may still contain bugs and unpolished features.

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6"
     alt="Get it on Obtinium"
     height="80">](<https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.example.memosm.insider%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fyamada-sexta%2Fmemos-m%22%2C%22author%22%3A%22yamada-sexta%22%2C%22name%22%3A%22MemosM%20(Insider)%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Insider%20Build*%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D>)

### Canary build

The Canary build is the "bleeding edge" version. It contains the very latest code, features and improvements, but it carries a higher risk of major regressions or crashes.

**Expect frequent updates, experimental changes and potential instability.**

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6"
     alt="Get it on Obtinium"
     height="80">](<https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.example.memosm.canary%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fyamada-sexta%2Fmemos-m%22%2C%22author%22%3A%22yamada-sexta%22%2C%22name%22%3A%22MemosM%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Latest*%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22MemosM%20(Canary)%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22A%20lightweight%2C%20feature-rich%20Android%20client%20for%20Memos.%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D>)

You can also download from [releases](https://github.com/yamada-sexta/memos-m/releases/tag/latest) directly.

On Android 17 and later, connecting to a local Memos server may require the **Nearby devices** permission. MemosM requests it when you sign in to a local server or open a saved local account. If access was denied, use **Local network access** on the login screen or in app settings to enable it. Public servers do not require this permission.

## Build from source

Requirements:

- JDK 21
- Android SDK with platform 37.1 (`compileSdk = 37`, `minSdk = 24`)
- Docker only for the API integration tests

```bash
# Debug build, installs alongside the other channels as <applicationId>.debug
./gradlew :app:assembleDebug

# Other channels
./gradlew :app:assembleInsider
./gradlew :app:assembleCanary
./gradlew :app:assembleRelease
```

APKs are split by ABI (`arm64-v8a` and `armeabi-v7a`); there is no universal APK. Outputs land in `app/build/outputs/apk/<build type>/`.

| Build type | Application ID | Label | R8 | Signing |
| --- | --- | --- | --- | --- |
| `debug` | `org.example.memosm.debug` | MemosM (Debug) | no | debug |
| `canary` | `org.example.memosm.canary` | MemosM | no | release |
| `insider` | `org.example.memosm.insider` | MemosM (Insider) | no | release |
| `release` | `org.example.memosm` | MemosM | yes | release |
| `benchmark` | `org.example.memosm` | MemosM | yes | release |

Release builds (including `canary`, `insider` and `benchmark`) are signed from `keystore.properties` at the repository root, or from the `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables used by CI — see `app/build.gradle.kts`. A debug build needs none of that.

`benchmark` is the release code made profileable without being debuggable, signed with the release key so it replaces an installed release build without losing data; it is what the macrobenchmark module drives.

## Testing

```bash
# JVM unit tests, no device required
./gradlew :app:testDebugUnitTest

# Compose UI / instrumentation tests, device or emulator
./gradlew :app:connectedDebugAndroidTest
```

`MemosApiIntegrationTest` starts real Memos containers through Testcontainers, so it needs Docker:

```bash
./gradlew :app:testDebugUnitTest --tests org.example.memosm.api.MemosApiIntegrationTest
```

That suite is part of the regular JVM test task; the nightly workflow also runs it explicitly against the canary image and every supported release image. The `baselineprofile` and `benchmark` modules hold the baseline profile generator and the macrobenchmarks.

## Project layout

| Path | Contents |
| --- | --- |
| `app/src/main/java/org/example/memosm/api` | Retrofit interfaces and the versioned server adapters (v0.26 to current, plus the unversioned fallback) |
| `app/src/main/java/org/example/memosm/data` | Caches, offline outbox, sync, backup, media, audit log and DataStore persistence |
| `app/src/main/java/org/example/memosm/viewmodel` | View model, list managers and the feature delegates |
| `app/src/main/java/org/example/memosm/ui` | Compose screens, components and the Miuix theme layer |
| `app/src/main/java/org/example/memosm/model` | API models and the structured filter model |
| `app/src/main/java/org/example/memosm/network` | Local-network permission handling |
| `app/src/main/java/org/example/memosm/widget` | Glance home-screen widgets |
| `app/src/main/res/values-*` | Translations |
| `baselineprofile`, `benchmark` | Baseline profile and macrobenchmark modules |
| `docs` | Design and implementation notes |

Design notes:

- [Filter system](docs/filter-system-refactor.md) — structured filters, CEL compilation, offline matching
- [Markdown rendering](docs/markdown-rendering.md)
- [Backup format](docs/backup.md)
- [Floating capsule bottom bar](docs/floating-capsule-highlight-card.md) — highlight, shadow and blur recipe
- [Page entries](docs/entry-inventory.md) — inventory of screens and how each one is reached

## Branches

- **`main`** — both design languages: Material 3 and Miuix, switchable in the appearance settings.
- **`small`** — this branch: Miuix only. The Material 3 path and the style switch are removed; everything else is kept in sync with `main`.

## Contributing

Contributions are welcome in the form of code, bug reports or feature suggestions.

- **Issues:** open an issue to discuss bugs or design ideas.
- **Pull requests:** focused, easy-to-review PRs are appreciated.
- **Translations:** there is no dedicated translation workflow yet; PRs against the `strings.xml` files in `app/src/main/res/values-*` are welcome.

## Related projects

- **Official Memos project:** [usememos.com](https://usememos.com/)
- **MoeMemos Android:** [github.com/mudkipme/MoeMemosAndroid](https://github.com/mudkipme/MoeMemosAndroid)

## License

[MIT](LICENSE)
