/**
 * Extracts a poster frame from a video URL by loading it offscreen
 * and capturing a canvas frame at 1 second in.
 * Results are cached in memory and localStorage.
 */
import { safeSetItem, safeGetItem } from "./safeStorage";

const CACHE_KEY = "videorelay_posters";
const memoryCache = new Map<string, string>();
const pendingFetches = new Map<string, Promise<string | null>>();

// Load persistent cache
try {
  const stored = JSON.parse(safeGetItem(CACHE_KEY) || "{}");
  for (const [k, v] of Object.entries(stored)) {
    if (typeof v === "string") memoryCache.set(k, v);
  }
} catch {}

function persistCache() {
  try {
    // Only keep last 200 entries
    const entries = [...memoryCache.entries()].slice(-200);
    safeSetItem(CACHE_KEY, JSON.stringify(Object.fromEntries(entries)));
  } catch {}
}

/**
 * Get a cached poster for a video ID, or null if not yet extracted.
 */
export function getCachedPoster(videoId: string): string | null {
  return memoryCache.get(videoId) || null;
}

/**
 * Extract a poster frame from a video URL.
 * Returns a data URL or null on failure.
 * Deduplicates concurrent requests for the same video.
 */
export function extractPoster(videoId: string, videoUrl: string): Promise<string | null> {
  if (memoryCache.has(videoId)) return Promise.resolve(memoryCache.get(videoId)!);

  // Don't try HLS streams
  if (videoUrl.includes(".m3u8")) return Promise.resolve(null);

  const existing = pendingFetches.get(videoId);
  if (existing) return existing;

  const promise = new Promise<string | null>((resolve) => {
    const video = document.createElement("video");
    video.muted = true;
    video.preload = "metadata";
    // Try with crossOrigin first for canvas access
    video.crossOrigin = "anonymous";

    const cleanup = () => {
      video.removeAttribute("src");
      video.load();
      pendingFetches.delete(videoId);
    };

    const timeout = setTimeout(() => {
      cleanup();
      resolve(null);
    }, 8000);

    video.onloadeddata = () => {
      // Seek to 1 second or 10% of duration
      video.currentTime = Math.min(1, video.duration * 0.1);
    };

    video.onseeked = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = Math.min(video.videoWidth, 640);
        canvas.height = Math.round(canvas.width * (video.videoHeight / video.videoWidth));
        const ctx = canvas.getContext("2d");
        if (ctx) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const dataUrl = canvas.toDataURL("image/jpeg", 0.7);
          memoryCache.set(videoId, dataUrl);
          persistCache();
          clearTimeout(timeout);
          cleanup();
          resolve(dataUrl);
          return;
        }
      } catch {}
      clearTimeout(timeout);
      cleanup();
      resolve(null);
    };

    video.onerror = () => {
      clearTimeout(timeout);
      cleanup();
      resolve(null);
    };

    video.src = videoUrl;
  });

  pendingFetches.set(videoId, promise);
  return promise;
}
