# VideoRelay for Android

Decentralized, censorship-resistant video for Nostr. YouTube without the gatekeepers.

## Features

- **Browse & Discover** — Home, Trending, Most Zapped, Following feeds with tag filtering
- **Watch** — ExoPlayer with HLS + progressive download, Picture-in-Picture
- **Shorts** — Vertical swipe feed for short-form video (≤30s)
- **Live** — NIP-53 live streams with viewer counts
- **Upload** — Blossom multi-server upload + NIP-71 event publishing
- **Offline** — Download videos for offline viewing via foreground service
- **Zap** — NIP-57 Lightning zaps via LNURL-pay
- **Comments** — NIP-22 threaded comments
- **Search** — NIP-50 relay search
- **Auth** — Amber (NIP-55) for signing, the standard Android Nostr signer
- **Relay Management** — Add/remove/toggle relays, smart defaults

## Tech Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You) |
| Video | Media3 ExoPlayer |
| DI | Hilt |
| DB | Room (video cache, downloads, view history, profiles) |
| Network | OkHttp WebSockets |
| Images | Coil |
| Navigation | Compose Navigation |
| Nostr | Custom implementation (NIP-71, NIP-57, NIP-55, NIP-53, NIP-22, NIP-50, NIP-98) |

## Nostr Protocol Support

| NIP | Purpose |
|-----|---------|
| NIP-71 | Video events (kind 21, 22, 34235, 34236) |
| NIP-55 | Android signing via Amber |
| NIP-57 | Zaps (kind 9734/9735 + LNURL-pay) |
| NIP-53 | Live streams (kind 30311) |
| NIP-22 | Comments (kind 1111) |
| NIP-50 | Search |
| NIP-98 | HTTP auth for Blossom uploads (kind 24242) |
| Kind 1 | Text note video detection |

## Architecture

```
app/
├── data/
│   ├── nostr/       # Relay pool, event parsing, NIPs
│   ├── blossom/     # Decentralized file upload
│   ├── db/          # Room database + entities
│   └── repository/  # Data layer abstraction
├── domain/model/    # Video, Profile, LiveStream, Comment
├── ui/
│   ├── theme/       # Material You theming
│   ├── components/  # Shared UI (VideoCard, shimmer, etc.)
│   ├── home/        # Feed with tabs + categories
│   ├── watch/       # Video player + comments
│   ├── shorts/      # Vertical pager
│   ├── live/        # Live streams
│   ├── upload/      # Blossom upload
│   ├── channel/     # Creator profiles
│   ├── search/      # NIP-50 search
│   ├── downloads/   # Offline videos
│   └── settings/    # Relays, account, theme
├── service/         # Download foreground service
├── navigation/      # Compose NavHost
└── di/              # Hilt modules
```

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (needs signing key)
./gradlew assembleRelease
```

## Distribution

Distributed via **Zapstore** (nostr-native app store). No Google Play Store needed.

- APK signed with project key
- Published as nostr event for discovery
- Updates pushed via nostr

## Default Relays

```
wss://relay.nostr.band
wss://relay.damus.io
wss://relay.primal.net
wss://relay.flare.pub
wss://video.nostr.build
wss://nos.lol
wss://relay.snort.social
wss://nostr.wine
wss://purplepag.es
wss://offchain.pub
```

## Requirements

- Android 8.0+ (API 26)
- Amber recommended for signing (NIP-55)
- Any Lightning wallet for zaps

## License

MIT
