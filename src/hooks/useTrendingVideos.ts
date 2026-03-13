import { useState, useEffect, useCallback } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, type ParsedVideo, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND } from "@/lib/nostr";
import type { Event } from "nostr-tools";

/**
 * Fetch trending/popular video content using nostr.band's NIP-50 search
 * and nostr.wine's trending API as supplementary discovery sources.
 * 
 * This finds content that the basic relay query misses by leveraging
 * aggregator relays that index the entire nostr network.
 */

const DISCOVERY_RELAYS = [
  "wss://relay.nostr.band",  // Indexes all known relays
  "wss://relay.primal.net",  // Large index
];

interface TrendingOptions {
  enabled?: boolean;
}

export function useTrendingVideos(options: TrendingOptions = {}) {
  const { enabled = true } = options;
  const [trendingVideos, setTrendingVideos] = useState<ParsedVideo[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchTrending = useCallback(async () => {
    if (!enabled) return;
    setLoading(true);

    try {
      const pool = getPool();
      const allEvents: Event[] = [];
      const seen = new Set<string>();

      // Strategy 1: Query aggregator relays with large limits and no time filter
      // These relays index everything, so we get much deeper content
      const bigQueryPromise = (async () => {
        try {
          const events = await pool.querySync(DISCOVERY_RELAYS, {
            kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND],
            limit: 500,
          });
          for (const e of events) {
            if (!seen.has(e.id)) {
              seen.add(e.id);
              allEvents.push(e);
            }
          }
        } catch (err) {
          console.warn("Big query failed:", err);
        }
      })();

      // Strategy 2: Query with popular hashtags to find categorized content
      const popularTags = ["bitcoin", "nostr", "video", "podcast", "music", "tutorial", "news", "tech", "privacy", "lightning"];
      const tagPromises = popularTags.map(async (tag) => {
        try {
          const events = await pool.querySync(DISCOVERY_RELAYS, {
            kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND],
            "#t": [tag],
            limit: 50,
          });
          for (const e of events) {
            if (!seen.has(e.id)) {
              seen.add(e.id);
              allEvents.push(e);
            }
          }
        } catch {}
      });

      // Strategy 3: Time-windowed queries to get content from different periods
      const now = Math.floor(Date.now() / 1000);
      const day = 86400;
      const timeWindows = [
        { since: now - 7 * day, until: now },          // last week
        { since: now - 30 * day, until: now - 7 * day }, // 1-4 weeks ago
        { since: now - 90 * day, until: now - 30 * day }, // 1-3 months ago
        { since: now - 365 * day, until: now - 90 * day }, // 3-12 months ago
      ];

      const timePromises = timeWindows.map(async ({ since, until }) => {
        try {
          const events = await pool.querySync(DISCOVERY_RELAYS.slice(0, 1), {
            kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND],
            since,
            until,
            limit: 100,
          });
          for (const e of events) {
            if (!seen.has(e.id)) {
              seen.add(e.id);
              allEvents.push(e);
            }
          }
        } catch {}
      });

      // Run all strategies in parallel
      await Promise.allSettled([bigQueryPromise, ...tagPromises, ...timePromises]);

      const parsed = allEvents
        .map(parseVideoEvent)
        .filter((v): v is ParsedVideo => v !== null);

      setTrendingVideos(parsed);
    } catch (err) {
      console.error("Trending fetch failed:", err);
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useEffect(() => {
    fetchTrending();
  }, [fetchTrending]);

  return { trendingVideos, loading, refetch: fetchTrending };
}
