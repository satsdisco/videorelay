/**
 * Curated creators system for VideoRelay.
 * Admin can pin creators whose videos get priority placement on the home page.
 * 
 * Primary storage: Nostr NIP-51 kind 30001 list (d-tag: "videorelay-curated")
 * Fallback: localStorage cache of last known list
 * Management: Settings > Curate tab (admin only)
 */

import { safeSetItem, safeGetItem } from "./safeStorage";

const CURATED_KEY = "videorelay_curated_creators";
const LIST_D_TAG = "videorelay-curated";
const LIST_KIND = 30001;

/** Admin pubkeys — only these users can see and manage the Curate tab */
export const ADMIN_PUBKEYS = new Set([
  "47276eb163fc54b3733930ab5cfd5fa94687a1953871a873ad4faee91e8a5f38", // satsdisco
]);

/** The admin pubkey we fetch the curated list from */
const CURATOR_PUBKEY = "47276eb163fc54b3733930ab5cfd5fa94687a1953871a873ad4faee91e8a5f38";

export interface CuratedCreator {
  pubkey: string;
  label?: string;
  addedAt: number;
}

// Hardcoded fallback — used if relay fetch fails and no localStorage cache
const FALLBACK_CREATORS: CuratedCreator[] = [
  { pubkey: "7807080250235b13c8fb67aeaf340f24c33845b2470e7ddc7ec0d40c00682645", label: "Video creator", addedAt: 0 },
];

/**
 * Fetch curated list from Nostr relays (NIP-51 kind 30001)
 * Returns the list of pubkeys from the latest event, or null if fetch fails.
 */
export async function fetchCuratedFromRelays(): Promise<CuratedCreator[] | null> {
  try {
    const { getPool, DEFAULT_RELAYS } = await import("./nostr");
    const pool = getPool();
    const events = await pool.querySync(DEFAULT_RELAYS, {
      kinds: [LIST_KIND],
      authors: [CURATOR_PUBKEY],
      "#d": [LIST_D_TAG],
      limit: 1,
    });

    if (events.length === 0) return null;

    // Sort by created_at desc, take latest
    const latest = events.sort((a, b) => b.created_at - a.created_at)[0];
    const creators: CuratedCreator[] = [];

    for (const tag of latest.tags) {
      if (tag[0] === "p" && tag[1]?.length === 64) {
        creators.push({
          pubkey: tag[1],
          label: tag[2] || undefined, // optional petname in tag[2]
          addedAt: latest.created_at * 1000,
        });
      }
    }

    // Cache to localStorage as fallback
    safeSetItem(CURATED_KEY, JSON.stringify(creators));
    return creators;
  } catch (err) {
    console.warn("Failed to fetch curated list from relays:", err);
    return null;
  }
}

/**
 * Get curated creators — tries localStorage cache first (fast),
 * then kicks off a relay fetch to update in background.
 */
export function getCuratedCreators(): CuratedCreator[] {
  try {
    const raw = safeGetItem(CURATED_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch {}
  return [...FALLBACK_CREATORS];
}

/**
 * Save curated creators to localStorage cache.
 */
export function saveCuratedCreators(creators: CuratedCreator[]): boolean {
  return safeSetItem(CURATED_KEY, JSON.stringify(creators));
}

/**
 * Add a creator to the local list (call publishCuratedList after to persist to Nostr)
 */
export function addCuratedCreator(pubkey: string, label?: string): boolean {
  const creators = getCuratedCreators();
  if (creators.some(c => c.pubkey === pubkey)) return true;
  creators.push({ pubkey, label, addedAt: Date.now() });
  return saveCuratedCreators(creators);
}

/**
 * Remove a creator from the local list (call publishCuratedList after to persist to Nostr)
 */
export function removeCuratedCreator(pubkey: string): boolean {
  const creators = getCuratedCreators().filter(c => c.pubkey !== pubkey);
  return saveCuratedCreators(creators);
}

export function getCuratedPubkeys(): string[] {
  return getCuratedCreators().map(c => c.pubkey);
}

/**
 * Publish the current curated list to Nostr relays as a NIP-51 kind 30001 event.
 * Requires NIP-07 extension (window.nostr) for signing.
 */
export async function publishCuratedList(): Promise<boolean> {
  if (!window.nostr) {
    console.error("No Nostr extension available for signing");
    return false;
  }

  try {
    const creators = getCuratedCreators();
    const tags: string[][] = [
      ["d", LIST_D_TAG],
      ...creators.map(c => c.label ? ["p", c.pubkey, c.label] : ["p", c.pubkey]),
    ];

    const unsignedEvent = {
      kind: LIST_KIND,
      created_at: Math.floor(Date.now() / 1000),
      tags,
      content: "", // NIP-51 public list — content is empty
    };

    const signedEvent = await window.nostr.signEvent(unsignedEvent);

    const { getPool, DEFAULT_RELAYS } = await import("./nostr");
    const pool = getPool();
    
    // Publish to all relays
    const results = await Promise.allSettled(
      DEFAULT_RELAYS.map(relay => pool.publish([relay], signedEvent))
    );

    const successes = results.filter(r => r.status === "fulfilled").length;
    console.log(`Published curated list to ${successes}/${DEFAULT_RELAYS.length} relays`);
    return successes > 0;
  } catch (err) {
    console.error("Failed to publish curated list:", err);
    return false;
  }
}

/**
 * Convert npub to hex pubkey (NIP-19 decode)
 */
export function npubToHex(npub: string): string | null {
  if (!npub.startsWith("npub1")) return npub; // assume already hex
  try {
    const CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    const data = npub.slice(5);
    const values: number[] = [];
    for (const c of data) {
      const v = CHARSET.indexOf(c);
      if (v === -1) return null;
      values.push(v);
    }
    const payload = values.slice(0, -6);
    let acc = 0;
    let bits = 0;
    const result: number[] = [];
    for (const v of payload) {
      acc = (acc << 5) | v;
      bits += 5;
      while (bits >= 8) {
        bits -= 8;
        result.push((acc >> bits) & 0xff);
      }
    }
    return result.map(b => b.toString(16).padStart(2, "0")).join("");
  } catch {
    return null;
  }
}
