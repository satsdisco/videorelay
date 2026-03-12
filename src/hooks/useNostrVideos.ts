import { useState, useEffect, useCallback } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, type ParsedVideo, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND } from "@/lib/nostr";

import type { Filter, Event } from "nostr-tools";

interface UseNostrVideosOptions {
  relays?: string[];
  limit?: number;
  authors?: string[];
  hashtag?: string;
  sortBy?: "recent" | "popular";
}

export function useNostrVideos(options: UseNostrVideosOptions = {}) {
  const {
    relays = DEFAULT_RELAYS,
    limit = 50,
    authors,
    hashtag,
    sortBy = "recent",
  } = options;

  const [videos, setVideos] = useState<ParsedVideo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchVideos = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const pool = getPool();

      const filter: Filter = {
        kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND],
        limit,
      };

      if (authors?.length) {
        filter.authors = authors;
      }

      if (hashtag) {
        filter["#t"] = [hashtag.toLowerCase()];
      }

      const events: Event[] = await pool.querySync(relays, filter);

      const parsed = events
        .map(parseVideoEvent)
        .filter((v): v is ParsedVideo => v !== null);

      // Fetch zap counts for discovered videos
      if (parsed.length > 0) {
        try {
          const videoIds = parsed.map((v) => v.id);
          const zapFilter: Filter = {
            kinds: [9735], // zap receipts
            "#e": videoIds,
            limit: 500,
          };
          const zapEvents = await pool.querySync(relays, zapFilter);

          // Count zaps per video and extract amounts
          const zapCounts = new Map<string, number>();
          for (const zap of zapEvents) {
            const eTag = zap.tags.find((t) => t[0] === "e");
            if (eTag) {
              const videoId = eTag[1];
              // Try to extract bolt11 amount from description
              let amount = 1; // default count
              const bolt11Tag = zap.tags.find((t) => t[0] === "bolt11");
              const descTag = zap.tags.find((t) => t[0] === "description");
              if (descTag) {
                try {
                  const desc = JSON.parse(descTag[1]);
                  const amountTag = desc.tags?.find((t: string[]) => t[0] === "amount");
                  if (amountTag) {
                    amount = Math.floor(parseInt(amountTag[1]) / 1000); // millisats to sats
                  }
                } catch {}
              }
              zapCounts.set(videoId, (zapCounts.get(videoId) || 0) + amount);
            }
          }

          // Apply zap counts
          for (const video of parsed) {
            video.zapCount = zapCounts.get(video.id) || 0;
          }
        } catch (err) {
          console.warn("Failed to fetch zap counts:", err);
        }
      }

      // Sort
      if (sortBy === "popular") {
        parsed.sort((a, b) => b.zapCount - a.zapCount || b.publishedAt - a.publishedAt);
      } else {
        parsed.sort((a, b) => b.publishedAt - a.publishedAt);
      }

      setVideos(parsed);
    } catch (err) {
      console.error("Error fetching video events:", err);
      setError("Failed to fetch videos from relays");
    } finally {
      setLoading(false);
    }
  }, [relays, limit, authors, hashtag, sortBy]);

  useEffect(() => {
    fetchVideos();
  }, [fetchVideos]);

  return { videos, loading, error, refetch: fetchVideos };
}
