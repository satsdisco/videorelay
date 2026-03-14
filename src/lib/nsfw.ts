/**
 * NSFW content detection for Nostr video events.
 * Checks NIP tags + keyword heuristics since most content isn't properly tagged.
 */

const NSFW_TAGS = new Set([
  "nsfw", "adult", "explicit", "porn", "xxx", "hentai", "r18", "18+",
  "content-warning", "sensitive", "nude", "nudity", "sex", "erotic",
  "lewd", "ecchi",
]);

const NSFW_TITLE_PATTERNS = [
  /\bnsfw\b/i,
  /\bhentai\b/i,
  /\bporn\b/i,
  /\bxxx\b/i,
  /\bnude\b/i,
  /\bnudity\b/i,
  /\bexplicit\b/i,
  /\berotic\b/i,
  /\badult.only\b/i,
  /\b18\+/i,
  /\blewd\b/i,
  /\becchi\b/i,
  /\buncensored\b/i,
  /\br18\b/i,
  /\br-18\b/i,
];

export interface NsfwCheckInput {
  title: string;
  summary?: string;
  tags: string[];
  /** Raw nostr event tags (array of arrays) for content-warning check */
  rawTags?: string[][];
}

export function isNsfw(video: NsfwCheckInput): boolean {
  // Check hashtags
  for (const tag of video.tags) {
    if (NSFW_TAGS.has(tag.toLowerCase())) return true;
  }

  // Check raw event tags for content-warning (NIP-36)
  if (video.rawTags) {
    for (const tag of video.rawTags) {
      if (tag[0] === "content-warning") return true;
      if (tag[0] === "L" && tag[1] === "content-warning") return true;
      if (tag[0] === "l" && tag[2] === "content-warning") return true;
    }
  }

  // Keyword check on title
  for (const pattern of NSFW_TITLE_PATTERNS) {
    if (pattern.test(video.title)) return true;
  }

  // Keyword check on summary/description
  if (video.summary) {
    for (const pattern of NSFW_TITLE_PATTERNS) {
      if (pattern.test(video.summary)) return true;
    }
  }

  return false;
}

const PREFS_KEY = "videorelay_prefs";

export function getNsfwBlurEnabled(): boolean {
  try {
    const s = localStorage.getItem(PREFS_KEY);
    if (s) {
      const prefs = JSON.parse(s);
      return prefs.nsfwBlur !== false; // default true
    }
  } catch {}
  return true; // default: blur enabled
}
