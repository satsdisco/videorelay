<div align="center">

<img src="public/logo.png" alt="VideoRelay" width="80" />

# VideoRelay

**A decentralized video platform built on Nostr.**

Browse, watch, upload, and zap videos — no accounts, no algorithms, no gatekeepers.

[![Live](https://img.shields.io/badge/Live-videorelay.lol-orange?style=flat-square)](https://videorelay.lol)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Built on Nostr](https://img.shields.io/badge/Built%20on-Nostr-purple?style=flat-square)](https://nostr.com)
[![NIP-71](https://img.shields.io/badge/NIP--71-Video%20Events-green?style=flat-square)](https://github.com/nostr-protocol/nips/blob/master/71.md)

[**→ Try it at videorelay.lol**](https://videorelay.lol)

</div>

---

## What is VideoRelay?

VideoRelay is a YouTube-style frontend for the [Nostr protocol](https://nostr.com). Video content is fetched directly from Nostr relays using [NIP-71](https://github.com/nostr-protocol/nips/blob/master/71.md) video events. There's no central server, no platform that can ban you, and no algorithm deciding what you see.

Your identity is your keypair. Your feed is your relay selection. Creator support goes directly through Lightning with no middleman taking a cut.

## Features

### 🎬 Discovery
- Home feed with trending, most-zapped, and following views
- Category filters — Bitcoin, Lightning, Nostr, Privacy, and more
- Shorts — vertical scroll feed for short-form content (TikTok-style)
- Live streams via NIP-53
- Featured Creators section (curated via NIP-51, admin-controlled)

### 📺 Playback
- Custom video player with HLS support
- Keyboard shortcuts, playback speed, PiP, fullscreen
- Blurred background for vertical videos
- Touch-optimized controls on mobile (tap to show/hide, double-tap to seek)

### ⚡ Creator Tools
- Upload videos to [Blossom](https://github.com/hzrd149/blossom) decentralized storage
- Publish NIP-71 video events signed with your Nostr extension
- Tag as Short, add categories, custom thumbnails
- Configurable Blossom media server (default: blossom.primal.net)

### 💸 Monetization
- Zap creators directly via Lightning (NIP-57)
- WebLN-native with bolt11 invoice fallback
- Zap amounts displayed on video cards

### 👤 Social
- Follow/unfollow creators (NIP-02 contact lists)
- Comments on videos (NIP-22)
- Channel pages with banner, bio, and video grid
- Profile resolution from relays

### ⚙️ Power User
- Relay manager — add, remove, toggle per-relay
- NSFW blur system — multi-layer detection with click-to-reveal
- Persistent auth across devices (pubkey saved in localStorage)
- Settings for playback, content preferences, and relay config

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | [React](https://react.dev) + [TypeScript](https://typescriptlang.org) |
| Build | [Vite](https://vitejs.dev) |
| Styling | [Tailwind CSS](https://tailwindcss.com) + [shadcn/ui](https://ui.shadcn.com) |
| Nostr | [nostr-tools](https://github.com/nbd-wtf/nostr-tools) |
| Video | HTML5 + [HLS.js](https://github.com/video-dev/hls.js) (lazy-loaded) |
| Hosting | [Vercel](https://vercel.com) |

## Nostr Protocol Support

| NIP | Description |
|-----|-------------|
| [NIP-01](https://github.com/nostr-protocol/nips/blob/master/01.md) | Basic protocol |
| [NIP-02](https://github.com/nostr-protocol/nips/blob/master/02.md) | Contact lists (following) |
| [NIP-07](https://github.com/nostr-protocol/nips/blob/master/07.md) | Browser extension signing |
| [NIP-19](https://github.com/nostr-protocol/nips/blob/master/19.md) | bech32 npub encoding |
| [NIP-22](https://github.com/nostr-protocol/nips/blob/master/22.md) | Comments |
| [NIP-36](https://github.com/nostr-protocol/nips/blob/master/36.md) | Sensitive content |
| [NIP-51](https://github.com/nostr-protocol/nips/blob/master/51.md) | Lists (curated creators) |
| [NIP-53](https://github.com/nostr-protocol/nips/blob/master/53.md) | Live activities |
| [NIP-57](https://github.com/nostr-protocol/nips/blob/master/57.md) | Lightning zaps |
| [NIP-71](https://github.com/nostr-protocol/nips/blob/master/71.md) | Video events |
| [NIP-94](https://github.com/nostr-protocol/nips/blob/master/94.md) | File metadata |
| [NIP-96](https://github.com/nostr-protocol/nips/blob/master/96.md) | File storage (Blossom) |

## Getting Started

### Prerequisites

- Node.js 18+
- A NIP-07 Nostr browser extension (for signing/uploading)

### Run locally

```bash
git clone https://github.com/satsdisco/videorelay.git
cd videorelay
npm install
npm run dev
```

Open [http://localhost:8080](http://localhost:8080).

### Sign in

VideoRelay uses [NIP-07](https://github.com/nostr-protocol/nips/blob/master/07.md) for authentication — no passwords, no emails.

Recommended extensions:
- [**Alby**](https://getalby.com) — browser extension, also handles Lightning
- [**nos2x**](https://github.com/nickolaev/nos2x-fox) — lightweight, Firefox + Chrome
- [**Nostore**](https://apps.apple.com/app/nostore/id1666553677) — Safari on macOS/iOS

### Deploy

```bash
npm run build
# Deploy dist/ to any static host, or use Vercel:
vercel --prod
```

Add a `vercel.json` SPA rewrite rule (already included in this repo).

## Architecture

```
src/
├── components/       # Reusable UI components
│   ├── VideoCard     # Thumbnail, title, zap count
│   ├── VideoPlayer   # Custom player with HLS support
│   ├── MiniSidebar   # Icon nav (collapsible to labels)
│   └── ...
├── hooks/            # Data fetching + state
│   ├── useNostrVideos      # Parallel relay querying
│   ├── usePopularVideos    # Engagement-scored discovery
│   ├── useNostrAuth        # NIP-07 session management
│   └── ...
├── lib/              # Core logic
│   ├── nostr.ts            # Relay pool, NIP-71 parsing
│   ├── videoDiscovery.ts   # Trending + engagement scoring
│   ├── posterCache.ts      # Offscreen poster extraction
│   ├── blossom.ts          # File upload with NIP-98 auth
│   ├── nsfw.ts             # Multi-layer content detection
│   └── safeStorage.ts      # localStorage quota management
└── pages/            # Route-level components
    ├── Index         # Home feed
    ├── Watch         # Video player page
    ├── Channel       # Creator profile
    ├── Shorts        # Vertical scroll feed
    ├── Upload        # Publish a video
    └── Settings      # Relays, playback, content prefs
```

## Engagement Scoring

VideoRelay ranks content using an engagement score with time decay:

```
score = (zapAmount + zapCount×100 + reactions×10 + reposts×50 - dislikes×200)
      × timeDecay(hours)
```

Where `timeDecay = 1 / (1 + hours/24)^0.5` — content doesn't permanently lose rank, but fresh content gets a boost.

## Contributing

Issues and PRs welcome. This is a public good project — if you're building on Nostr video, feel free to fork, extend, or take pieces for your own client.

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes
4. Open a PR

## License

[MIT](LICENSE) — do whatever you want with it.

---

<div align="center">

Built on [Nostr](https://nostr.com) · Powered by [Lightning](https://lightning.network) · Hosted on [Vercel](https://vercel.com)

**[videorelay.lol](https://videorelay.lol)**

</div>
