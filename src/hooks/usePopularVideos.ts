import { useState, useEffect, useCallback, useRef } from "react";
import { discoverPopularVideos } from "@/lib/videoDiscovery";
import { type ParsedVideo } from "@/lib/nostr";
import type { TimePeriod } from "./useNostrVideos";

interface UsePopularVideosOptions {
  relays: string[];
  timePeriod?: TimePeriod;
  enabled?: boolean;
  sortBy?: "trending" | "zaps";
}

export function usePopularVideos(options: UsePopularVideosOptions) {
  const { relays, timePeriod = "all", enabled = true, sortBy = "trending" } = options;
  const [videos, setVideos] = useState<ParsedVideo[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fetchedRef = useRef(false);
  const lastKeyRef = useRef("");

  const fetchKey = `${relays.join("|")}:${timePeriod}:${sortBy}:${enabled}`;

  const fetch = useCallback(async () => {
    if (!enabled || relays.length === 0) return;
    setLoading(true);
    setError(null);

    try {
      const results = await discoverPopularVideos(relays, {
        timePeriod,
        limit: sortBy === "zaps" ? 500 : 300,
        sortBy,
      });
      setVideos(results);
    } catch (err) {
      console.error("Popular videos fetch failed:", err);
      setError("Failed to fetch popular videos");
    } finally {
      setLoading(false);
    }
  }, [relays, timePeriod, sortBy, enabled]);

  useEffect(() => {
    if (fetchKey !== lastKeyRef.current) {
      lastKeyRef.current = fetchKey;
      fetchedRef.current = false;
    }
    if (!fetchedRef.current && enabled) {
      fetchedRef.current = true;
      fetch();
    }
  }, [fetchKey, enabled, fetch]);

  return { videos, loading, error, refetch: fetch };
}
