/**
 * Curated creators system for VideoRelay.
 * Admin can pin creators whose videos get priority placement on the home page.
 * Stored in localStorage, managed via Settings > Admin panel.
 */

const CURATED_KEY = "videorelay_curated_creators";

export interface CuratedCreator {
  pubkey: string;
  /** Optional label for the admin panel */
  label?: string;
  addedAt: number;
}

/**
 * Suggested creators — seeded on first visit if no curated list exists.
 * These are known Nostr video creators with regular content.
 */
const SEED_CREATORS: CuratedCreator[] = [
  { pubkey: "7807080250235b13c8fb67aeaf340f24c33845b2470e7ddc7ec0d40c00682645", label: "Video creator", addedAt: Date.now() },
];

export function getCuratedCreators(): CuratedCreator[] {
  try {
    const raw = localStorage.getItem(CURATED_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  // First visit — seed with defaults
  saveCuratedCreators(SEED_CREATORS);
  return [...SEED_CREATORS];
}

export function saveCuratedCreators(creators: CuratedCreator[]): void {
  localStorage.setItem(CURATED_KEY, JSON.stringify(creators));
}

export function addCuratedCreator(pubkey: string, label?: string): void {
  const creators = getCuratedCreators();
  if (creators.some(c => c.pubkey === pubkey)) return;
  creators.push({ pubkey, label, addedAt: Date.now() });
  saveCuratedCreators(creators);
}

export function removeCuratedCreator(pubkey: string): void {
  const creators = getCuratedCreators().filter(c => c.pubkey !== pubkey);
  saveCuratedCreators(creators);
}

export function getCuratedPubkeys(): string[] {
  return getCuratedCreators().map(c => c.pubkey);
}

/**
 * Convert npub to hex pubkey (NIP-19 decode)
 */
export function npubToHex(npub: string): string | null {
  if (!npub.startsWith("npub1")) return npub; // assume already hex
  try {
    // Simple bech32 decode for npub
    const CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    const data = npub.slice(5); // remove "npub1"
    const values: number[] = [];
    for (const c of data) {
      const v = CHARSET.indexOf(c);
      if (v === -1) return null;
      values.push(v);
    }
    // Remove checksum (last 6 values)
    const payload = values.slice(0, -6);
    // Convert 5-bit to 8-bit
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
