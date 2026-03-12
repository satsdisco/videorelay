import { SimplePool, type Filter, type Event } from "nostr-tools";

// Default relays — prioritizing video-heavy and popular relays
export const DEFAULT_RELAYS = [
  "wss://relay.damus.io",
  "wss://relay.nostr.band",
  "wss://nos.lol",
  "wss://relay.snort.social",
  "wss://nostr.wine",
  "wss://relay.primal.net",
  "wss://nostr-pub.wellorder.net",
  "wss://nostr.fmt.wiz.biz",
  "wss://relay.noswhere.com",
  "wss://nostr.mom",
  "wss://relay.mostr.pub",
  "wss://offchain.pub",
  "wss://relay.nostrati.com",
];

// Video event kinds per NIP-71
export const VIDEO_KIND = 21; // regular video
export const SHORT_VIDEO_KIND = 22; // short video
export const ADDRESSABLE_VIDEO_KIND = 34235; // addressable video
export const ADDRESSABLE_SHORT_KIND = 34236; // addressable short

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
export function parseVideoEvent(event: Event): ParsedVideo | null {
  const getTag = (name: string): string | undefined => {
    const tag = event.tags.find((t) => t[0] === name);
    return tag?.[1];
  };

  const getAllTags = (name: string): string[] => {
    return event.tags.filter((t) => t[0] === name).map((t) => t[1]);
  };

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

  const title = getTag("title") || getTag("subject") || "Untitled Video";
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
