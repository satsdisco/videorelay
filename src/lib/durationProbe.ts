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

export interface VideoMeta {
  duration: number; // seconds
  width: number;
  height: number;
  isVertical: boolean;
}

const metaCache = new Map<string, VideoMeta>();
const META_CACHE_KEY = "videorelay_vmeta";

// Load meta cache
try {
  const stored = JSON.parse(localStorage.getItem(META_CACHE_KEY) || "{}");
  for (const [k, v] of Object.entries(stored)) {
    if (v && typeof v === "object") metaCache.set(k, v as VideoMeta);
  }
} catch {}

function persistMeta() {
  try {
    const entries = [...metaCache.entries()].slice(-500);
    localStorage.setItem(META_CACHE_KEY, JSON.stringify(Object.fromEntries(entries)));
  } catch {}
}

/** Get cached full metadata */
export function getCachedMeta(videoId: string): VideoMeta | null {
  return metaCache.get(videoId) ?? null;
}

/** Probe a video URL for duration AND dimensions. */
export function probeVideo(videoId: string, videoUrl: string): Promise<VideoMeta | null> {
  if (metaCache.has(videoId)) return Promise.resolve(metaCache.get(videoId)!);

  // Skip HLS
  if (videoUrl.includes(".m3u8")) return Promise.resolve(null);

  const existing = pending.get(videoId);
  if (existing) return existing as Promise<VideoMeta | null>;

  const promise = new Promise<VideoMeta | null>((resolve) => {
    const video = document.createElement("video");
    video.preload = "metadata";
    video.muted = true;

    const timeout = setTimeout(() => {
      cleanup();
      resolve(null);
    }, 8000);

    const cleanup = () => {
      video.removeAttribute("src");
      video.load();
      pending.delete(videoId);
    };

    video.onloadedmetadata = () => {
      const dur = video.duration;
      const w = video.videoWidth;
      const h = video.videoHeight;
      clearTimeout(timeout);
      cleanup();

      if (dur && isFinite(dur) && dur > 0) {
        const meta: VideoMeta = {
          duration: Math.round(dur),
          width: w,
          height: h,
          isVertical: h > w,
        };
        memCache.set(videoId, Math.round(dur));
        metaCache.set(videoId, meta);
        persist();
        persistMeta();
        resolve(meta);
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

  pending.set(videoId, promise as Promise<number | null>);
  return promise;
}

/** Legacy: probe just duration */
export function probeDuration(videoId: string, videoUrl: string): Promise<number | null> {
  return probeVideo(videoId, videoUrl).then(m => m?.duration ?? null);
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
