# Maddookie — Native Android & TV Anime Client (AniList + MyAnimeList Sync)

**Maddookie** (`com.maddookie`) is a fast, ad-free anime streaming and tracking app for Android phones, tablets, and Android TV, built with **Kotlin + Jetpack Compose** and a **Potato-Turbo Low-RAM Engine**.

## Direct APK Download (v1.0.1-maddookie-turbo • versionCode 98)
- **Download Signed APK (`maddookie.apk`)**: [Google Drive Direct Download](https://drive.google.com/file/d/1Z68Az30FM80VxE_vJmYWcM4Zt0CRMvjN/view?usp=drivesdk)
- **Package ID**: `com.maddookie` (`minSdkVersion 21`, Android 5.0–15 + Android TV, 4-byte zipaligned, v1 + v2 signed)

## Key Features
- **Two-Way AniList & MyAnimeList (MAL) Sync**: OAuth2 PKCE authentication (`maddookie://auth` & `maddookie://mal-auth`) and redirect-free Simkl Device PIN authentication (`simkl.com/pin`) with automatic progress sync at 85% watched.
- **Multi-Provider Streaming Engine**: Automatic fallback across **AllAnime**, **AnimePahe**, **Anikoto**, **AnimeKai**, and **KickAssAnime** with SUB/DUB switching and HLS/MP4 playback.
- **Discord Rich Presence**: Live activity status cards with custom Maddookie artwork.
- **Potato-Turbo Low-RAM Optimization**: Tuned for instant cold start and zero-jank scrolling on Xiaomi Redmi 14C (Helio G81 Ultra / HyperOS) and constrained Android devices (RGB_565 bitmap decoding, 12% heap cap, deferred background WebView initialization, and zero-blur GPU fast path).

## Building from Source
```bash
./gradlew assembleMobileDebug assembleTvDebug
```

## Pushing Full Local Source Bundle (`maddookie_android_app.zip`)
Extract `Maddookie-main/` from `maddookie_android_app.zip` and run:
```bash
bash push_to_github.sh flamewall-ship-it Maddookie
```
