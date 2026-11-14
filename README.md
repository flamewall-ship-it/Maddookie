# Maddookie — Native Android & TV Anime Client

**Maddookie** is a fast, ad-free anime discovery, local tracking, and streaming app built with **Kotlin + Jetpack Compose**, **Material Design 3**, **Media3 ExoPlayer**, **Room**, and a **Potato-Turbo Low-RAM Engine**.

## Features Implemented
- **Keyless Public Catalog & Weekly Airing Schedule**: Uses public, unauthenticated AniList GraphQL queries (`https://graphql.anilist.co`) with zero API keys, zero OAuth credentials, and zero login requirements.
- **Local Room Watchlist & JSON Backup**:
  - On-device Room watchlist (`Watching`, `Plan to Watch`, `Completed`, `On Hold`, `Dropped`) with `+1` episode increment and automatic **85% watched** episode tracking.
  - Zero-permission **Export / Import JSON** backup via Android Storage Access Framework (`CreateDocument` / `OpenDocument`).
- **Multi-Provider Streaming Engine & Media3 Player**:
  - Automatic fallback pipeline across **AllAnime**, **AnimePahe**, **Anikoto**, **AnimeKai**, and **KickAssAnime** with **SUB/DUB** switching, **HLS/MP4** playback via Media3 ExoPlayer, and `+85s` OP skip.
- **Discord Rich Presence Preview**: Local activity preview card with custom Maddookie artwork and configurable episode/provider badges.
- **Potato-Turbo Low-RAM Engine**:
  - Runtime **RGB_565** bitmap decoding and configurable **12% heap cache cap** in Coil.
  - Deferred background WebView initialization toggle, zero-blur GPU fast path, and live JVM heap / device RAM telemetry.
- **Hardened Build & Transport**: Enforces HTTPS-only TLS (`usesCleartextTraffic="false"`), `allowBackup="false"`, and R8 release minification with zero secrets in code or APK.

## Building
```bash
gradle :app:assembleDebug
```
