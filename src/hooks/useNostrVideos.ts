import { useState, useEffect, useCallback } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, type ParsedVideo, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND } from "@/lib/nostr";
import type { Filter, Event } from "nostr-tools";

interface UseNostrVideosOptions {
  relays?: string[];
  limit?: number;
  authors?: string[];
  hashtag?: string;
}

export function useNostrVideos(options: UseNostrVideosOptions = {}) {
  const {
    relays = DEFAULT_RELAYS,
    limit = 50,
    authors,
    hashtag,
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
        .filter((v): v is ParsedVideo => v !== null)
        .sort((a, b) => b.publishedAt - a.publishedAt);

      setVideos(parsed);
    } catch (err) {
      console.error("Error fetching video events:", err);
      setError("Failed to fetch videos from relays");
    } finally {
      setLoading(false);
    }
  }, [relays, limit, authors, hashtag]);

  useEffect(() => {
    fetchVideos();
  }, [fetchVideos]);

  return { videos, loading, error, refetch: fetchVideos };
}
