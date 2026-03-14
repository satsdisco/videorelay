/**
 * Probe real video duration by loading metadata in an offscreen video element.
 * Results cached in localStorage so we only probe each URL once.
 */

const CACHE_KEY = "videorelay_durations";
const memCache = new Map<string, number>();
const pending = new Map<string, Promise<number | null>>();

// Load from localStorage
try {
  const stored = JSON.parse(localStorage.getItem(CACHE_KEY) || "{}");
  for (const [k, v] of Object.entries(stored)) {
    if (typeof v === "number") memCache.set(k, v);
  }
} catch {}

function persist() {
  try {
    const entries = [...memCache.entries()].slice(-500);
    localStorage.setItem(CACHE_KEY, JSON.stringify(Object.fromEntries(entries)));
  } catch {}
}

/** Get cached duration in seconds, or null if not probed yet. */
export function getCachedDuration(videoId: string): number | null {
  return memCache.get(videoId) ?? null;
}

/** Probe a video URL for its duration. Returns seconds or null on failure. */
export function probeDuration(videoId: string, videoUrl: string): Promise<number | null> {
  if (memCache.has(videoId)) return Promise.resolve(memCache.get(videoId)!);

  // Skip HLS — can't probe m3u8 this way
  if (videoUrl.includes(".m3u8")) return Promise.resolve(null);

  const existing = pending.get(videoId);
  if (existing) return existing;

  const promise = new Promise<number | null>((resolve) => {
    const video = document.createElement("video");
    video.preload = "metadata";
    video.muted = true;

    const timeout = setTimeout(() => {
      cleanup();
      resolve(null);
    }, 6000);

    const cleanup = () => {
      video.removeAttribute("src");
      video.load();
      pending.delete(videoId);
    };

    video.onloadedmetadata = () => {
      const dur = video.duration;
      clearTimeout(timeout);
      cleanup();
      if (dur && isFinite(dur) && dur > 0) {
        memCache.set(videoId, Math.round(dur));
        persist();
        resolve(Math.round(dur));
      } else {
        resolve(null);
      }
    };

    video.onerror = () => {
      clearTimeout(timeout);
      cleanup();
      resolve(null);
    };

    video.src = videoUrl;
  });

  pending.set(videoId, promise);
  return promise;
}

/** Format seconds into human-readable duration */
export function formatDurationSecs(secs: number): string {
  if (secs <= 0) return "";
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
