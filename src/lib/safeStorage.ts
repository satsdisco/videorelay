/**
 * Safe localStorage wrapper that handles quota errors gracefully.
 * When quota is exceeded, it evicts least important caches first.
 */

// Priority order for eviction (lowest = evicted first)
// Keys MUST match actual localStorage keys used in the codebase
const EVICTION_ORDER = [
  "videorelay_posters",          // poster frame cache — HUGE (base64 images) — posterCache.ts
  "videorelay_durations",        // duration/dimension probe cache — durationProbe.ts
  "videorelay_engagement",       // engagement scores — videoDiscovery.ts
  "videorelay_video_cache",      // video event cache — useNostrVideos.ts
  "videorelay_views",            // view history — viewTracker.ts
  // Never evict: relay config, theme, prefs, curated creators, auth
];

function evictCaches(): boolean {
  for (const key of EVICTION_ORDER) {
    if (localStorage.getItem(key)) {
      localStorage.removeItem(key);
      return true; // freed space
    }
  }
  return false; // nothing left to evict
}

export function safeSetItem(key: string, value: string): boolean {
  for (let attempt = 0; attempt < EVICTION_ORDER.length + 1; attempt++) {
    try {
      localStorage.setItem(key, value);
      return true;
    } catch (e) {
      if (e instanceof DOMException && e.name === "QuotaExceededError") {
        if (!evictCaches()) return false; // nothing left to evict
      } else {
        return false;
      }
    }
  }
  return false;
}

export function safeGetItem(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}
