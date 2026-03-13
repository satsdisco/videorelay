import { SimplePool, type Filter, type Event } from "nostr-tools";

// Default relays — discovery relays first (index everything), then video-focused, then popular
export const DEFAULT_RELAYS = [
  // Discovery/aggregator relays — index events from thousands of relays
  "wss://relay.nostr.band",
  "wss://relay.damus.io",
  "wss://relay.primal.net",
  // Video-focused relays
  "wss://relay.flare.pub",
  "wss://video.nostr.build",
  // Popular general relays
  "wss://nos.lol",
  "wss://relay.snort.social",
  "wss://nostr.wine",
  "wss://purplepag.es",
  "wss://offchain.pub",
];

// Video event kinds per NIP-71
export const VIDEO_KIND = 21; // regular video
export const SHORT_VIDEO_KIND = 22; // short video
export const ADDRESSABLE_VIDEO_KIND = 34235; // addressable video
export const ADDRESSABLE_SHORT_KIND = 34236; // addressable short

// All video kinds including kind 1 (text notes with video URLs)
export const ALL_VIDEO_KINDS = [1, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND];

// Singleton pool
let pool: SimplePool | null = null;

export function getPool(): SimplePool {
  if (!pool) {
    pool = new SimplePool();
  }
  return pool;
}

export interface ParsedVideo {
  id: string;
  pubkey: string;
  title: string;
  summary: string;
  thumbnail: string;
  videoUrl: string;
  duration: string;
  durationSeconds: number;
  publishedAt: number;
  tags: string[];
  zapCount: number;
  isShort: boolean;
  rawEvent: Event;
}

/**
 * Parse a NIP-71 video event into a structured format
 */
// Regex to find video URLs in text content
const VIDEO_URL_REGEX = /https?:\/\/[^\s"'<>]+\.(mp4|webm|mov|m3u8|ogg)(\?[^\s"'<>]*)?/gi;
const IMAGE_URL_REGEX = /https?:\/\/[^\s"'<>]+\.(jpg|jpeg|png|webp|gif)(\?[^\s"'<>]*)?/gi;

export function parseVideoEvent(event: Event): ParsedVideo | null {
  const getTag = (name: string): string | undefined => {
    const tag = event.tags.find((t) => t[0] === name);
    return tag?.[1];
  };

  const getAllTags = (name: string): string[] => {
    return event.tags.filter((t) => t[0] === name).map((t) => t[1]);
  };

  // For kind 1 events, detect video URLs in content
  if (event.kind === 1) {
    const videoMatches = event.content?.match(VIDEO_URL_REGEX);
    if (!videoMatches || videoMatches.length === 0) {
      // Also check for url tags pointing to video files
      const urlTag = getTag("url");
      if (!urlTag || !urlTag.match(/\.(mp4|webm|mov|m3u8|ogg)(\?|$)/i)) {
        return null; // Not a video post
      }
    }
  }

  // Get video URL from various possible tag formats
  let videoUrl = getTag("url") || "";

  // Check imeta tags for video URL
  if (!videoUrl) {
    const imetaTags = event.tags.filter((t) => t[0] === "imeta");
    for (const imeta of imetaTags) {
      for (const field of imeta.slice(1)) {
        if (field.startsWith("url ")) {
          const url = field.replace("url ", "");
          // Check if it looks like a video
          if (
            url.match(/\.(mp4|webm|mov|m3u8|ogg)(\?|$)/i) ||
            url.includes("video")
          ) {
            videoUrl = url;
            break;
          }
        }
      }
      if (videoUrl) break;
    }
  }

  // Also check for streaming URL
  if (!videoUrl) {
    videoUrl = getTag("streaming") || getTag("recording") || "";
  }

  // For kind 1: extract video URL from content text
  if (!videoUrl && event.kind === 1 && event.content) {
    const matches = event.content.match(VIDEO_URL_REGEX);
    if (matches) videoUrl = matches[0];
  }

  if (!videoUrl) return null;

  // Get thumbnail
  let thumbnail = getTag("thumb") || getTag("image") || getTag("thumbnail") || "";
  if (!thumbnail) {
    // Check imeta for image
    const imetaTags = event.tags.filter((t) => t[0] === "imeta");
    for (const imeta of imetaTags) {
      for (const field of imeta.slice(1)) {
        if (field.startsWith("url ")) {
          const url = field.replace("url ", "");
          if (url.match(/\.(jpg|jpeg|png|webp|gif)(\?|$)/i)) {
            thumbnail = url;
            break;
          }
        }
      }
      if (thumbnail) break;
    }
  }

  // For kind 1: try to extract thumbnail from image URLs in content
  if (!thumbnail && event.kind === 1 && event.content) {
    const imageMatches = event.content.match(IMAGE_URL_REGEX);
    if (imageMatches) thumbnail = imageMatches[0];
  }

  // For kind 1: use first line of content as title (strip URLs)
  let title = getTag("title") || getTag("subject") || "";
  if (!title && event.kind === 1 && event.content) {
    const firstLine = event.content.split("\n")[0]
      .replace(VIDEO_URL_REGEX, "")
      .replace(IMAGE_URL_REGEX, "")
      .replace(/https?:\/\/[^\s]+/g, "")
      .trim();
    title = firstLine || "Untitled Video";
  }
  if (!title) title = "Untitled Video";

  const summary = event.content || getTag("summary") || "";
  const duration = getTag("duration") || "0";
  const hashtags = getAllTags("t");

  const durationSecs = parseInt(duration) || 0;
  const isShortKind = event.kind === SHORT_VIDEO_KIND || event.kind === ADDRESSABLE_SHORT_KIND;
  const isShort = isShortKind || (durationSecs > 0 && durationSecs <= 60);

  return {
    id: event.id,
    pubkey: event.pubkey,
    title,
    summary,
    thumbnail,
    videoUrl,
    duration: formatDuration(durationSecs),
    durationSeconds: durationSecs,
    publishedAt: event.created_at,
    tags: hashtags,
    zapCount: 0,
    isShort,
    rawEvent: event,
  };
}

function formatDuration(seconds: number): string {
  if (!seconds) return "";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function timeAgo(timestamp: number): string {
  const now = Math.floor(Date.now() / 1000);
  const diff = now - timestamp;
  if (diff < 60) return "just now";
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  if (diff < 604800) return `${Math.floor(diff / 86400)}d ago`;
  if (diff < 2592000) return `${Math.floor(diff / 604800)}w ago`;
  return `${Math.floor(diff / 2592000)}mo ago`;
}

export function shortenNpub(pubkey: string): string {
  return pubkey.slice(0, 8) + "..." + pubkey.slice(-4);
}
