import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, type ParsedVideo, ALL_VIDEO_KINDS } from "@/lib/nostr";
import { safeSetItem, safeGetItem } from "@/lib/safeStorage";
import { cacheVideos } from "@/lib/videoCache";
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

// localStorage cache for video events — persists across sessions
const VIDEO_CACHE_KEY = "videorelay_video_cache";
const CACHE_MAX_AGE = 1000 * 60 * 30; // 30 minutes

function loadCachedEvents(): ParsedVideo[] {
  try {
    const raw = safeGetItem(VIDEO_CACHE_KEY);
    if (!raw) return [];
    const { videos, timestamp } = JSON.parse(raw);
    if (Date.now() - timestamp > CACHE_MAX_AGE) return [];
    return videos || [];
  } catch {
    return [];
  }
}

function saveCachedEvents(videos: ParsedVideo[]) {
  try {
    // Only cache the most recent 500 to keep storage reasonable
    const trimmed = videos.slice(0, 500);
    safeSetItem(VIDEO_CACHE_KEY, JSON.stringify({
      videos: trimmed,
      timestamp: Date.now(),
    }));
  } catch {}
}

/**
 * Query relays independently and merge results.
 * This prevents slow relays from blocking fast ones.
 */
async function queryRelaysIndependently(
  relays: string[],
  filter: Filter,
  timeoutMs = 10000,
): Promise<Event[]> {
  const pool = getPool();
  const allEvents: Event[] = [];
  const seen = new Set<string>();

  const addEvents = (events: Event[]) => {
    for (const e of events) {
      if (!seen.has(e.id)) {
        seen.add(e.id);
        allEvents.push(e);
      }
    }
  };

  // Query each relay independently with a per-relay timeout
  const promises = relays.map((relay) =>
    Promise.race([
      pool.querySync([relay], filter).then(addEvents),
      new Promise<void>((resolve) => setTimeout(resolve, timeoutMs)),
    ]).catch(() => {})
  );

  // Wait for all to complete (or timeout)
  await Promise.allSettled(promises);
  return allEvents;
}

export function useNostrVideos(options: UseNostrVideosOptions = {}) {
  const {
    relays = DEFAULT_RELAYS,
    limit = 200,
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
  const allFetchedRef = useRef<ParsedVideo[]>([]);

  const relaysSignature = relays.join("|");
  const authorsSignature = authors?.join("|") ?? "";

  const stableRelays = useMemo(() => relays, [relaysSignature]);
  const stableAuthors = useMemo(() => authors, [authorsSignature]);

  const buildFilter = useCallback((until?: number, customLimit?: number): Filter => {
    const filter: Filter = {
      kinds: ALL_VIDEO_KINDS,
      limit: customLimit || limit,
    };

    if (stableAuthors?.length) {
      filter.authors = stableAuthors;
    }

    if (hashtag) {
      filter["#t"] = [hashtag.toLowerCase()];
    }

    if (search?.trim()) {
      (filter as any).search = search.trim();
    }

    const sincePeriod = getTimePeriodSince(timePeriod);
    if (sincePeriod) {
      filter.since = sincePeriod;
    }

    if (until) {
      filter.until = until;
    }

    return filter;
  }, [limit, stableAuthors, hashtag, search, timePeriod]);

  const fetchZapCounts = useCallback(async (parsed: ParsedVideo[]) => {
    if (parsed.length === 0) return;
    try {
      const pool = getPool();
      const videoIds = parsed.map((v) => v.id);
      const chunkSize = 50;
      for (let i = 0; i < videoIds.length; i += chunkSize) {
        const chunk = videoIds.slice(i, i + chunkSize);
        const zapFilter: Filter = {
          kinds: [9735],
          "#e": chunk,
          limit: 500,
        };

        // Query zaps from aggregator relays for best coverage
        const zapRelays = stableRelays.filter(r =>
          r.includes("nostr.band") || r.includes("primal") || r.includes("nostr.wine")
        );
        const relaysToQuery = zapRelays.length > 0 ? zapRelays : stableRelays.slice(0, 3);
        const zapEvents = await getPool().querySync(relaysToQuery, zapFilter);

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
    allFetchedRef.current = [];

    try {
      if (stableRelays.length === 0) {
        setVideos([]);
        setError("No relays enabled. Enable at least one relay in Relay Settings.");
        return;
      }

      // Load cache for instant display while fetching fresh data
      const cached = loadCachedEvents();
      const isDefaultFetch = !stableAuthors && !hashtag && !search;
      if (cached.length > 0 && isDefaultFetch) {
        setVideos(sortVideos(cached));
        setLoading(false); // Show content immediately
      }

      // Fetch fresh data from all relays (waits for all with 10s timeout each)
      const filter = buildFilter(undefined, limit);
      const events = await queryRelaysIndependently(stableRelays, filter);

      const parsed = events
        .map(parseVideoEvent)
        .filter((v): v is ParsedVideo => v !== null);

      let deduped = dedupeVideos(parsed);

      // Merge with cache — never show fewer results than we had
      if (isDefaultFetch && cached.length > 0) {
        const freshIds = new Set(deduped.map(v => v.id));
        const cachedExtras = cached.filter(v => !freshIds.has(v.id));
        if (cachedExtras.length > 0) {
          deduped = dedupeVideos([...deduped, ...cachedExtras]);
        }
      }

      cacheVideos(deduped);
      allFetchedRef.current = deduped;

      if (deduped.length > 0) {
        oldestTimestamp.current = Math.min(...deduped.map((v) => v.publishedAt));
      }
      if (parsed.length < limit) {
        setHasMore(false);
      }

      setVideos(sortVideos(deduped));

      if (isDefaultFetch) {
        saveCachedEvents(deduped);
      }

      // Background: fetch zap counts then re-sort
      fetchZapCounts(deduped).then(() => {
        setVideos(prev => sortVideos([...prev]));
        if (isDefaultFetch) {
          saveCachedEvents(deduped);
        }
      });
    } catch (err) {
      console.error("Error fetching video events:", err);
      setError("Failed to fetch videos from relays");
    } finally {
      setLoading(false);
    }
  }, [stableRelays, limit, buildFilter, fetchZapCounts, sortVideos, stableAuthors, hashtag, search]);

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore || !oldestTimestamp.current) return;

    setLoadingMore(true);
    try {
      const filter = buildFilter(oldestTimestamp.current - 1, limit);
      const events = await queryRelaysIndependently(stableRelays, filter);

      const parsed = events
        .map(parseVideoEvent)
        .filter((v): v is ParsedVideo => v !== null);

      if (parsed.length === 0 || parsed.length < 10) {
        setHasMore(false);
      }

      if (parsed.length > 0) {
        await fetchZapCounts(parsed);
        allFetchedRef.current = dedupeVideos([...allFetchedRef.current, ...parsed]);
        oldestTimestamp.current = Math.min(...parsed.map((v) => v.publishedAt));
        setVideos(sortVideos(allFetchedRef.current));
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
