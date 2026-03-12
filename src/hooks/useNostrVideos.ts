import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, type ParsedVideo, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND } from "@/lib/nostr";

import type { Filter, Event } from "nostr-tools";

export type TimePeriod = "today" | "week" | "month" | "year" | "all";

interface UseNostrVideosOptions {
  relays?: string[];
  limit?: number;
  authors?: string[];
  hashtag?: string;
  sortBy?: "recent" | "popular";
  search?: string;
  timePeriod?: TimePeriod;
}

function getTimePeriodSince(period: TimePeriod): number | undefined {
  if (period === "all") return undefined;
  const now = Math.floor(Date.now() / 1000);
  const day = 86400;
  switch (period) {
    case "today": return now - day;
    case "week": return now - 7 * day;
    case "month": return now - 30 * day;
    case "year": return now - 365 * day;
  }
}

// Deduplicate videos by id
function dedupeVideos(videos: ParsedVideo[]): ParsedVideo[] {
  const seen = new Set<string>();
  return videos.filter((v) => {
    if (seen.has(v.id)) return false;
    seen.add(v.id);
    return true;
  });
}

export function useNostrVideos(options: UseNostrVideosOptions = {}) {
  const {
    relays = DEFAULT_RELAYS,
    limit = 100,
    authors,
    hashtag,
    sortBy = "recent",
    search,
    timePeriod = "all",
  } = options;

  const [videos, setVideos] = useState<ParsedVideo[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const oldestTimestamp = useRef<number | null>(null);

  const relaysSignature = relays.join("|");
  const authorsSignature = authors?.join("|") ?? "";

  const stableRelays = useMemo(() => relays, [relaysSignature]);
  const stableAuthors = useMemo(() => authors, [authorsSignature]);

  const buildFilter = useCallback((until?: number): Filter => {
    const filter: Filter = {
      kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND],
      limit,
    };

    if (stableAuthors?.length) {
      filter.authors = stableAuthors;
    }

    if (hashtag) {
      filter["#t"] = [hashtag.toLowerCase()];
    }

    // NIP-50 search — relays that support it will use it, others ignore it
    if (search?.trim()) {
      (filter as any).search = search.trim();
    }

    if (until) {
      filter.until = until;
    }

    return filter;
  }, [limit, stableAuthors, hashtag, search]);

  const fetchZapCounts = useCallback(async (parsed: ParsedVideo[]) => {
    if (parsed.length === 0) return;
    try {
      const pool = getPool();
      const videoIds = parsed.map((v) => v.id);
      // Batch in chunks of 50 to avoid huge filters
      const chunkSize = 50;
      for (let i = 0; i < videoIds.length; i += chunkSize) {
        const chunk = videoIds.slice(i, i + chunkSize);
        const zapFilter: Filter = {
          kinds: [9735],
          "#e": chunk,
          limit: 500,
        };
        const zapEvents = await pool.querySync(stableRelays, zapFilter);

        const zapCounts = new Map<string, number>();
        for (const zap of zapEvents) {
          const eTag = zap.tags.find((t) => t[0] === "e");
          if (eTag) {
            const videoId = eTag[1];
            let amount = 1;
            const descTag = zap.tags.find((t) => t[0] === "description");
            if (descTag) {
              try {
                const desc = JSON.parse(descTag[1]);
                const amountTag = desc.tags?.find((t: string[]) => t[0] === "amount");
                if (amountTag) {
                  amount = Math.floor(parseInt(amountTag[1]) / 1000);
                }
              } catch {}
            }
            zapCounts.set(videoId, (zapCounts.get(videoId) || 0) + amount);
          }
        }

        for (const video of parsed) {
          if (zapCounts.has(video.id)) {
            video.zapCount = zapCounts.get(video.id) || 0;
          }
        }
      }
    } catch (err) {
      console.warn("Failed to fetch zap counts:", err);
    }
  }, [stableRelays]);

  const sortVideos = useCallback((vids: ParsedVideo[]) => {
    const sorted = [...vids];
    if (sortBy === "popular") {
      sorted.sort((a, b) => b.zapCount - a.zapCount || b.publishedAt - a.publishedAt);
    } else {
      sorted.sort((a, b) => b.publishedAt - a.publishedAt);
    }
    return sorted;
  }, [sortBy]);

  const fetchVideos = useCallback(async () => {
    setLoading(true);
    setError(null);
    setHasMore(true);
    oldestTimestamp.current = null;

    try {
      if (stableRelays.length === 0) {
        setVideos([]);
        setError("No relays enabled. Enable at least one relay in Relay Settings.");
        return;
      }

      const pool = getPool();
      const filter = buildFilter();
      const events: Event[] = await pool.querySync(stableRelays, filter);

      const parsed = events
        .map(parseVideoEvent)
        .filter((v): v is ParsedVideo => v !== null);

      const deduped = dedupeVideos(parsed);

      await fetchZapCounts(deduped);

      // Track oldest for pagination
      if (deduped.length > 0) {
        oldestTimestamp.current = Math.min(...deduped.map((v) => v.publishedAt));
      }
      if (deduped.length < limit) {
        setHasMore(false);
      }

      setVideos(sortVideos(deduped));
    } catch (err) {
      console.error("Error fetching video events:", err);
      setError("Failed to fetch videos from relays");
    } finally {
      setLoading(false);
    }
  }, [stableRelays, limit, buildFilter, fetchZapCounts, sortVideos]);

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore || !oldestTimestamp.current) return;

    setLoadingMore(true);
    try {
      const pool = getPool();
      const filter = buildFilter(oldestTimestamp.current - 1);
      const events: Event[] = await pool.querySync(stableRelays, filter);

      const parsed = events
        .map(parseVideoEvent)
        .filter((v): v is ParsedVideo => v !== null);

      if (parsed.length === 0 || parsed.length < limit) {
        setHasMore(false);
      }

      if (parsed.length > 0) {
        await fetchZapCounts(parsed);
        oldestTimestamp.current = Math.min(...parsed.map((v) => v.publishedAt));

        setVideos((prev) => sortVideos(dedupeVideos([...prev, ...parsed])));
      }
    } catch (err) {
      console.warn("Error loading more videos:", err);
    } finally {
      setLoadingMore(false);
    }
  }, [loadingMore, hasMore, stableRelays, limit, buildFilter, fetchZapCounts, sortVideos]);

  useEffect(() => {
    fetchVideos();
  }, [fetchVideos]);

  return { videos, loading, loadingMore, error, hasMore, refetch: fetchVideos, loadMore };
}
