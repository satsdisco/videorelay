/**
 * Safe localStorage wrapper that handles quota errors gracefully.
 * When quota is exceeded, it evicts least important caches first.
 */

// Priority order for eviction (lowest = evicted first)
const EVICTION_ORDER = [
  "videorelay_poster_cache",     // poster frame cache — regenerable
  "videorelay_duration_cache",   // duration probe cache — regenerable
  "videorelay_meta_cache",       // video meta cache — regenerable
  "videorelay_engagement",       // engagement scores — regenerable
  "videorelay_video_cache",      // video event cache — regenerable
  "videorelay_views",            // view history — nice to have
  // Never evict: relay config, theme, prefs, curated creators
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
