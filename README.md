# VideoRelay

A decentralized, censorship-resistant video platform built on [Nostr](https://nostr.com). Browse, watch, publish, and zap videos — no accounts, no algorithms, no gatekeepers.

**🌐 [videorelay.lol](https://videorelay.lol)**

## What is this?

VideoRelay is a YouTube-style frontend that pulls video content directly from Nostr relays using [NIP-71](https://github.com/nostr-protocol/nips/blob/master/71.md) video events. Your identity is your Nostr keypair. Your feed is your relay selection. Your support goes directly to creators via Lightning zaps.

## Features

- **Browse & discover** — Video grid with infinite scroll, category/hashtag filtering, sort by recent or most zapped
- **Shorts** — TikTok-style vertical scroll for short-form content
- **Watch** — Custom video player with keyboard shortcuts, PiP, playback speed, fullscreen
- **Publish** — Create NIP-71 video events signed with your Nostr extension
- **Zap creators** — Lightning-powered tipping (NIP-57)
- **Channel pages** — Browse videos by creator with profile display
- **Comments** — Read and post comments on videos
- **Follow/unfollow** — Manage your contact list (kind 3)
- **Relay manager** — Add, remove, and toggle relays
- **Search** — NIP-50 relay search + client-side filtering
- **Mobile-first** — Responsive layout with bottom nav and touch gestures

## Tech Stack

- [React](https://react.dev) + [TypeScript](https://typescriptlang.org)
- [Vite](https://vitejs.dev) — build tooling
- [Tailwind CSS](https://tailwindcss.com) — styling
- [shadcn/ui](https://ui.shadcn.com) — component primitives
- [nostr-tools](https://github.com/nbd-wtf/nostr-tools) — Nostr protocol
- [TanStack Query](https://tanstack.com/query) — data fetching

## Getting Started

```bash
# Clone the repo
git clone https://github.com/satsdisco/videorelay.git
cd videorelay

# Install dependencies
npm install

# Start dev server
npm run dev
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

### Sign in

VideoRelay uses [NIP-07](https://github.com/nostr-protocol/nips/blob/master/07.md) browser extensions for authentication. Install one of:

- [Alby](https://getalby.com) (also handles Lightning zaps)
- [nos2x](https://github.com/nickolaev/nos2x-fox)
- [Nostore](https://apps.apple.com/app/nostore/id1666553677) (Safari)

## Nostr Protocol

VideoRelay works with these NIPs:

| NIP | Purpose |
|-----|---------|
| [NIP-07](https://github.com/nostr-protocol/nips/blob/master/07.md) | Browser extension login |
| [NIP-50](https://github.com/nostr-protocol/nips/blob/master/50.md) | Relay search |
| [NIP-57](https://github.com/nostr-protocol/nips/blob/master/57.md) | Lightning zaps |
| [NIP-71](https://github.com/nostr-protocol/nips/blob/master/71.md) | Video events (kinds 21, 22, 34235, 34236) |

## Contributing

This project is open source and contributions are welcome. Feel free to open issues, submit PRs, or fork and build your own thing.

## License

MIT

## Links

- **Live:** [videorelay.lol](https://videorelay.lol)
- **Nostr:** Built for the Nostr ecosystem
- **Lightning:** Zap-powered, no middlemen
