<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="128" alt="MemosM App Icon" />

# MemosM

A feature-rich Android client for Memos.

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/yamada-sexta/memos-m/canary-build.yml?style=flat-square)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/yamada-sexta/memos-m/nightly-test.yml?style=flat-square&label=nightly%20API%20tests)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/yamada-sexta/memos-m/total?style=flat-square)
![GitHub License](https://img.shields.io/github/license/yamada-sexta/memos-m?style=flat-square)
![GitHub Repo stars](https://img.shields.io/github/stars/yamada-sexta/memos-m?style=flat-square)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/yamada-sexta/memos-m?style=flat-square)

</div>

---

**MemosM** is an Android client for [Memos](https://usememos.com/) that follows the current server releases. It supports Memos v0.26 and newer: the version probe reads the server version and picks the matching adapter (v0.26–v0.30), while newer or unknown servers use the latest adapter. The nightly integration suite runs the adapters against v0.26.0 up to the current stable and canary images. Android 7.0 (API 24) and newer are supported.

It focuses on a native experience, speed, and full support for current Memos features, with an offline-first data layer and a structured filter system.

## Screenshots

<img height="300"  alt="image" src="https://github.com/user-attachments/assets/aba051d0-0c6d-4d0a-9bd0-9b7d0389d000" />
<img height="300"  alt="image" src="https://github.com/user-attachments/assets/c1c3ac79-1d17-402a-b538-1eafe6b624e8" />

## Why MemosM?

While other clients like [MoeMemos](https://github.com/mudkipme/MoeMemosAndroid) are available, MemosM was developed to address specific technical and user experience gaps:

- **Native Android Focus:** MemosM is built specifically for Android rather than being a cross-platform port. This ensures better performance and robust tablet support.
- **Efficiency:** Optimized syncing prevents high data usage and ensures a faster, more responsive experience compared to clients that re-sync entire histories.
- **Offline-first by default:** a local-first cache, queued offline writes, conflict handling, and optional text/attachment pre-download keep the app usable without a connection.
- **Filtering that fits:** saved views, a collapsible tag tree, date windows, content types and ordering — compiled to the server's CEL filter and mirrored locally for offline use.
- **Modern Feature Set:** By focusing on recent Memos versions, we avoid the limitations of backward compatibility and can implement modern features like advanced Markdown, Reactions and SSO.
- **Two design languages:** Material 3 and Miuix (HyperOS), with Material You, custom and monochrome palettes.

## SSO login

Select **SSO**, enter your Memos server URL, and choose **Find providers**. Select a provider
to authenticate in your browser. MemosM then creates a personal access token for the account,
just as password login does. Password and manual token login remain available.

An administrator must add the app callback URI to the allowed redirect URIs for the OAuth2
client configured in Memos. Keep the existing Memos web callback registered as well.

| Build | Callback URI |
| --- | --- |
| Release | `org.example.memosm://oauth/callback` |
| Insider | `org.example.memosm.insider://oauth/callback` |
| Canary | `org.example.memosm.canary://oauth/callback` |
| Debug | `org.example.memosm.debug://oauth/callback` |

The provider must accept custom URI schemes and S256 PKCE. Providers requiring an HTTPS
callback can use manual token login instead. SSO is supported by the v0.26+ adapters and the
newer Memos SSO format. Registration and identity provisioning follow the server's settings.
Sign-in attempts expire after ten minutes; if Android terminates the app during authentication,
start sign-in again.

## Downloads

### Stable Build

> MemosM is still under active development, so there is no stable build for now.

### Insider Build

The Insider build is designed for users who want to test out relatively new features without facing constant instability. While generally usable, it may still contain bugs and unpolished features.

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6"
     alt="Get it on Obtinium"
     height="80">](<https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.example.memosm.insider%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fyamada-sexta%2Fmemos-m%22%2C%22author%22%3A%22yamada-sexta%22%2C%22name%22%3A%22MemosM%20(Insider)%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Insider%20Build*%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D>)

### Canary Build

The Canary build is the "bleeding edge" version. It contains the very latest code, features, and improvements, but it carries a higher risk of major regressions or crashes.

**Expect: Frequent updates, experimental changes, and potential instability.**

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6"
     alt="Get it on Obtinium"
     height="80">](<https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.example.memosm.canary%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fyamada-sexta%2Fmemos-m%22%2C%22author%22%3A%22yamada-sexta%22%2C%22name%22%3A%22MemosM%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Latest*%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22MemosM%20(Canary)%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22A%20lightweight%2C%20feature-rich%20Android%20client%20for%20Memos.%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D>)

You can also download from [release](https://github.com/yamada-sexta/memos-m/releases/tag/latest) directly.

On Android 17 and later, connecting to a local Memos server may require the **Nearby devices** permission. MemosM requests it when you sign in to a local server or open a saved local account. If access was denied, use **Local network access** on the login screen or in app settings to enable it. Public servers do not require this permission.

---

## Features

- **Accounts & sign-in**
  - Token, password, or SSO login
  - Multiple accounts with fast switching; caches and filters are per account
  - Local network access permission for local servers on Android 17 and later
- **Memos**
  - Compose, edit, delete, pin, archive and unarchive memos
  - Share sheet target for text, images and files
  - Comments, reactions (using the server's emoji list), linked memos and location
  - Search over content, tags and date ranges, with ordering
  - Structured filter panel: saved views, collapsible tag tree, date presets and custom ranges,
    content types (links/code/todos) and ordering. Applied filters persist per account and stay
    visible as removable tokens under the top bar
- **Markdown**
  - GFM rendering: headings, lists, task lists, tables, quotes, links and images
  - Code blocks with syntax highlighting, optional line numbers and line wrapping
  - LaTeX math, link previews, paragraph indent and CJK spacing options
- **Media**
  - Full-screen image viewer with zoom; avatar cropping
  - Video playback (ExoPlayer) and audio recording/playback
  - Attachments browser, streamed downloads, and a size-limited offline media cache
- **Organize**
  - Tag tree in the profile, memo kinds (links, code, todos), archived memos, statistics and activity
  - Notifications list (no background polling)
- **Offline & sync**
  - Local-first cache with offline reads for the feed, search, map and attachments
  - Offline writes queued and replayed on reconnect, with a conflict-resolution dialog
  - Optional pre-download of text and attachments (Wi-Fi only), cache size limits
  - Sync status panel, audit log, failed-upload retry/discard
- **Appearance**
  - Material 3 and Miuix (HyperOS) styles; light, dark or follow-system
  - Material You, custom and monochrome palettes; font and heading-size options
  - Navigation layout for phones and tablets (bottom bar or rail), edit gestures, pull-to-refresh
- **Widgets & shortcuts**
  - Draft widget and user-stats widget (with its own configuration screen)
  - Four launcher shortcuts: new memo, drafts, notifications, search
- **Backup & restore**
  - `.mmbackup` export with MessagePack + gzip and optional password protection, covering accounts,
    settings, caches, media and drafts — see [the backup notes](docs/backup.md)
- **Map**
  - Native map with Memos / Explore / All scopes, location pins, saved views, and composing at a
    chosen place
  - OpenFreeMap basemaps with an OpenStreetMap raster fallback
- **Languages**
  - English, German, Japanese, Korean, Polish, Simplified Chinese and Traditional Chinese

The Map destination appears only after the connected instance accepts the `has_location`
memo filter. Older and unknown instances do not show it. Previously verified accounts can
browse cached locations offline using available cached tiles. Saved views require a connection;
map regions are not downloaded in advance.

## Non-goals

- **Indefinite Backward Compatibility:** We prioritize compatibility with the latest Memos API to leverage modern features. Older versions of Memos will not be supported for long.
- **Multiplatform Support:** MemosM is built exclusively for Android to ensure a high-quality, native experience.

## Build from source

Requirements:

- JDK 21
- Android SDK with platform 37.1 (`compileSdk = 37`, `minSdk = 24`)
- Docker only for the API integration tests

```bash
# Debug build (installs alongside the other channels as <applicationId>.debug)
./gradlew :app:assembleDebug

# Other channels
./gradlew :app:assembleInsider
./gradlew :app:assembleCanary
./gradlew :app:assembleRelease
```

The debug APK is written to `app/build/outputs/apk/debug/`. Release builds are signed from
`keystore.properties` at the repository root, or from the `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS` and `KEY_PASSWORD` environment variables used by CI (see `app/build.gradle.kts`);
none of that is required for a debug build.

## Testing

```bash
# JVM unit tests (no device required)
./gradlew :app:testDebugUnitTest

# Compose UI / instrumentation tests (device or emulator)
./gradlew :app:connectedDebugAndroidTest
```

`MemosApiIntegrationTest` starts real Memos containers through Testcontainers, so it needs Docker:

```bash
./gradlew :app:testDebugUnitTest --tests org.example.memosm.api.MemosApiIntegrationTest
```

That suite runs as part of the regular JVM test task but needs Docker; the nightly workflow also
runs it explicitly against the canary image and every supported release image.

## Project layout

| Path | Contents |
| --- | --- |
| `app/src/main/java/org/example/memosm/api` | Retrofit interfaces and the versioned server adapters (v0.26 to current, plus the unversioned fallback) |
| `app/src/main/java/org/example/memosm/data` | Caches, offline outbox, sync, backup, media and DataStore persistence |
| `app/src/main/java/org/example/memosm/viewmodel` | View model, list managers and the feature delegates |
| `app/src/main/java/org/example/memosm/ui` | Compose screens, components and the Material 3 / Miuix theme layer |
| `app/src/main/java/org/example/memosm/model` | API models and the structured filter model |
| `app/src/main/java/org/example/memosm/network` | Local-network permission handling |
| `app/src/main/java/org/example/memosm/widget` | Glance home-screen widgets |
| `docs` | Design and implementation notes |

Design notes:

- [Filter system](docs/filter-system-refactor.md) — structured filters, CEL compilation, offline matching
- [Markdown rendering](docs/markdown-rendering.md)
- [Miuix theme plan](docs/miuix-refactor-plan.md)
- [Backup format](docs/backup.md)
- [Entry inventory](docs/entry-inventory.md)

## Contributing

Contributions are welcome in the form of code, bug reports, or feature suggestions.

- **Issues:** Open an issue to discuss bugs or design ideas.
- **Pull Requests:** Focused, easy-to-review PRs are appreciated.
- **Translations:** There is no dedicated translation workflow yet; PRs against the `strings.xml` files in `app/src/main/res/values-*` are welcome.

## Related Projects

- **Official Memos Project:** [usememos.com](https://usememos.com/)
- **MoeMemos Android:** [github.com/mudkipme/MoeMemosAndroid](https://github.com/mudkipme/MoeMemosAndroid)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yamada-sexta/memos-m&type=date&legend=top-left)](https://www.star-history.com/#yamada-sexta/memos-m&type=date&legend=top-left)

## Backup and restore

Export configurable `.mmbackup` files with MessagePack, gzip, and optional password protection.
Restore selected accounts, settings, cached server data, media, and unpublished drafts. Queued
edits are optional and unchecked by default; restored edits are kept for review and never sync
automatically. Android Auto Backup restores account information during device setup, while
direct device transfer also preserves caches and drafts. The server remains authoritative.
See [backup format and verification](docs/backup.md) for details.

## License

[MIT](LICENSE)
