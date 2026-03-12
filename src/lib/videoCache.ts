import type { ParsedVideo } from "@/lib/nostr";

// Global in-memory video cache — persists across navigations
const videoCache = new Map<string, ParsedVideo>();

export function cacheVideos(videos: ParsedVideo[]) {
  for (const v of videos) {
    videoCache.set(v.id, v);
  }
}

export function getCachedVideo(id: string): ParsedVideo | null {
  return videoCache.get(id) ?? null;
}

export function getCachedVideos(): ParsedVideo[] {
  return Array.from(videoCache.values());
}
