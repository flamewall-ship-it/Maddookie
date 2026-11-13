# Maddookie — Native Android & TV Anime Client

**Maddookie** is a fast, ad-free anime discovery, tracking, and streaming app built with **Kotlin + Jetpack Compose**, **Material Design 3**, **Media3 ExoPlayer**, **Room**, and a **Potato-Turbo Low-RAM Engine**.

## Features Implemented
- **Live AniList Catalog & Weekly Airing Schedule**: Powered by AniList's GraphQL API (`https://graphql.anilist.co`) with Trending, Seasonal, Top Rated, live search, genre filtering, and 7-day airing schedule countdowns.
- **Two-Way AniList, MyAnimeList & Simkl Sync**:
  - OAuth2 PKCE deep-link flows (`maddookie://auth` and `maddookie://mal-auth`) plus direct AniList personal token verification.
  - Redirect-free **Simkl Device PIN** authorization (`simkl.com/pin`).
  - Local Room watchlist & playback persistence (`Watching`, `Plan to Watch`, `Completed`, `On Hold`, `Dropped`) with automatic progress sync at **85% watched**.
- **Multi-Provider Streaming Engine & Media3 Player**:
  - Automatic fallback pipeline across **AllAnime**, **AnimePahe**, **Anikoto**, **AnimeKai**, and **KickAssAnime** with **SUB/DUB** switching, **HLS/MP4** playback via Media3 ExoPlayer, and `+85s` OP skip.
- **Discord Rich Presence Card**: Live activity preview card with custom Maddookie artwork and configurable episode/provider badges.
- **Potato-Turbo Low-RAM Engine**:
  - Runtime **RGB_565** bitmap decoding and configurable **12% heap cache cap** in Coil.
  - Deferred background WebView initialization toggle, zero-blur GPU fast path, and live JVM heap / device RAM telemetry.
- **Adaptive Mobile, Tablet & TV Layout**: `enableEdgeToEdge()`, `WindowSizeClass` switching between `NavigationBar` and `NavigationRail`, and D-Pad friendly cards.

## Building
```bash
gradle :app:assembleDebug
```
