// Fun loading messages about Nostr's decentralized nature
const loadingMessages = [
  "Asking 6 relays nicely for your videos...",
  "Decentralization takes a sec — no CEO to yell at here",
  "Fetching from relays across the globe... patience, sovereign one",
  "No central server means no single point of failure... or speed",
  "Negotiating with independent relay operators worldwide...",
  "Your ISP can't block what it can't find 😏",
  "Loading at the speed of censorship resistance...",
  "Each relay is run by a real human. They're doing their best.",
  "Still faster than getting verified on Twitter...",
  "Satoshi waited 10 minutes per block. You can wait a few seconds.",
  "Distributing trust across multiple relays... trust no one, verify everything",
  "The relay hamsters are running as fast as they can 🐹",
  "If this were centralized, it'd be faster. But also... capturable.",
  "Somewhere, a relay operator just restarted their Raspberry Pi",
  "No algorithm deciding what you see. Just pure, unfiltered relay chaos.",
  "Proof of work: waiting for WebSocket connections",
  "Remember: not your keys, not your videos. We're fetching YOUR videos.",
  "Broadcasting to the Nostr-verse... beep boop zap ⚡",
];

const emptyMessages = [
  "The relays are empty... like Satoshi's known identity",
  "No videos here yet. Be the first to publish — you ARE the algorithm.",
  "Zero results. The decentralized void stares back at you.",
  "Nothing found. Even relays need content creators, anon.",
  "Looks like this corner of the Nostr-verse is uncharted territory 🗺️",
];

const errorMessages = [
  "Relays ghosted us. Classic decentralized behavior.",
  "Connection failed — probably a relay operator touching grass 🌱",
  "The relays said no. Very sovereign of them, honestly.",
  "WebSocket went AWOL. Even protocols need a break sometimes.",
  "Error 404: Centralized infrastructure not found (that's the point)",
];

export function getRandomLoadingMessage(): string {
  return loadingMessages[Math.floor(Math.random() * loadingMessages.length)];
}

export function getRandomEmptyMessage(): string {
  return emptyMessages[Math.floor(Math.random() * emptyMessages.length)];
}

export function getRandomErrorMessage(): string {
  return errorMessages[Math.floor(Math.random() * errorMessages.length)];
}
