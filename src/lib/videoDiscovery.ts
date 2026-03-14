import { getPool, parseVideoEvent, type ParsedVideo, ALL_VIDEO_KINDS } from "./nostr";
import type { Filter, Event } from "nostr-tools";

// Aggregator relays that index the broadest content
const DISCOVERY_RELAYS = [
  "wss://relay.damus.io",
  "wss://nos.lol",
  "wss://relay.primal.net",
  "wss://relay.snort.social",
  "wss://nostr.wine",
  "wss://offchain.pub",
  "wss://nostr21.com",
];

// Cache engagement scores in memory and localStorage
const ENGAGEMENT_CACHE_KEY = "videorelay_engagement";
const ENGAGEMENT_CACHE_TTL = 1000 * 60 * 15; // 15 min

interface EngagementScore {
  likes: number;
  dislikes: number;
  comments: number;
  reposts: number;
  zapAmount: number;
  zapCount: number;
  score: number; // composite
  ratioed: boolean;
}

let engagementCache: Map<string, EngagementScore> = new Map();
let cacheTimestamp = 0;

function loadEngagementCache() {
  try {
    const raw = localStorage.getItem(ENGAGEMENT_CACHE_KEY);
    if (!raw) return;
    const { data, timestamp } = JSON.parse(raw);
    if (Date.now() - timestamp > ENGAGEMENT_CACHE_TTL) return;
    engagementCache = new Map(Object.entries(data));
    cacheTimestamp = timestamp;
  } catch {}
}

function saveEngagementCache() {
  try {
    const obj: Record<string, EngagementScore> = {};
    engagementCache.forEach((v, k) => { obj[k] = v; });
    localStorage.setItem(ENGAGEMENT_CACHE_KEY, JSON.stringify({
      data: obj,
      timestamp: Date.now(),
    }));
  } catch {}
}

/**
 * Compute engagement scores for a set of video events.
 * Fetches reactions (kind 7), reposts (kind 6), replies (kind 1), and zaps (kind 9735).
 */
async function fetchEngagementScores(videoIds: string[]): Promise<Map<string, EngagementScore>> {
  const pool = getPool();
  const scores = new Map<string, EngagementScore>();

  const zero = (): EngagementScore => ({
    likes: 0, dislikes: 0, comments: 0, reposts: 0,
    zapAmount: 0, zapCount: 0, score: 0, ratioed: false,
  });

  // Initialize all with zero scores
  for (const id of videoIds) {
    if (engagementCache.has(id)) {
      scores.set(id, engagementCache.get(id)!);
    } else {
      scores.set(id, zero());
    }
  }

  // Only fetch for uncached IDs
  const uncached = videoIds.filter(id => !engagementCache.has(id));
  if (uncached.length === 0) return scores;

  // Batch in chunks
  const chunkSize = 50;
  for (let i = 0; i < uncached.length; i += chunkSize) {
    const chunk = uncached.slice(i, i + chunkSize);

    try {
      // Fetch reactions (kind 7), reposts (kind 6, 16), comments (kind 1111, 1), and zaps (kind 9735)
      // Use broader relay set for engagement queries — zaps especially need good coverage
      const engagementRelays = [...new Set([
        ...DISCOVERY_RELAYS,
        "wss://relay.nostr.band",
        "wss://relay.damus.io",
        "wss://nostr.wine",
        "wss://cache2.primal.net/v1",
      ])].slice(0, 6);

      const [reactions, comments, zaps] = await Promise.allSettled([
        pool.querySync(engagementRelays, {
          kinds: [7, 6, 16],
          "#e": chunk,
          limit: 2000,
        }),
        pool.querySync(engagementRelays, {
          kinds: [1111, 1],
          "#e": chunk,
          limit: 1000,
        }),
        pool.querySync(engagementRelays, {
          kinds: [9735],
          "#e": chunk,
          limit: 2000,
        }),
      ]);

      // Count reactions (likes/dislikes) and reposts
      if (reactions.status === "fulfilled") {
        for (const event of reactions.value) {
          const eTag = event.tags.find(t => t[0] === "e");
          if (!eTag) continue;
          const videoId = eTag[1];
          const entry = scores.get(videoId);
          if (!entry) continue;

          if (event.kind === 7) {
            // NIP-25: "+" or "" = like, "-" = dislike
            if (event.content === "-") {
              entry.dislikes++;
            } else {
              entry.likes++;
            }
          } else if (event.kind === 6 || event.kind === 16) {
            entry.reposts++;
          }
        }
      }

      // Count comments
      if (comments.status === "fulfilled") {
        for (const event of comments.value) {
          const eTag = event.tags.find(t => t[0] === "e");
          if (!eTag) continue;
          const videoId = eTag[1];
          const entry = scores.get(videoId);
          if (entry) entry.comments++;
        }
      }

      // Count zaps and amounts
      if (zaps.status === "fulfilled") {
        for (const event of zaps.value) {
          const eTag = event.tags.find(t => t[0] === "e");
          if (!eTag) continue;
          const videoId = eTag[1];
          const entry = scores.get(videoId);
          if (!entry) continue;

          entry.zapCount++;
          const descTag = event.tags.find(t => t[0] === "description");
          if (descTag) {
            try {
              const desc = JSON.parse(descTag[1]);
              const amountTag = desc.tags?.find((t: string[]) => t[0] === "amount");
              if (amountTag) {
                entry.zapAmount += Math.floor(parseInt(amountTag[1]) / 1000);
              }
            } catch {}
          }
        }
      }
    } catch (err) {
      console.warn("Engagement fetch error:", err);
    }
  }

  // Compute composite scores and cache
  for (const [id, entry] of scores) {
    // Ratioed: dislikes > likes * 2 → exclude from trending
    entry.ratioed = entry.dislikes > entry.likes * 2 && entry.dislikes > 3;

    // Scoring: likes*0.5 - dislikes*2 + (zapAmount/1000)*5 + comments*0.3 + reposts*1
    entry.score = (entry.likes * 0.5)
      - (entry.dislikes * 2)
      + (entry.zapAmount / 1000) * 5
      + (entry.comments * 0.3)
      + (entry.reposts * 1);

    engagementCache.set(id, entry);
  }
  saveEngagementCache();

  return scores;
}

/**
 * Deep crawl for video content across all time periods.
 * Returns videos with real engagement scores.
 */
export async function discoverPopularVideos(
  relays: string[],
  options: {
    timePeriod?: "today" | "week" | "month" | "year" | "all";
    limit?: number;
    sortBy?: "trending" | "zaps";
  } = {}
): Promise<ParsedVideo[]> {
  const { timePeriod = "all", limit = 300, sortBy = "trending" } = options;
  loadEngagementCache();

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

  const now = Math.floor(Date.now() / 1000);
  const day = 86400;

  // Use discovery relays + user's relays for maximum coverage
  const queryRelays = [...new Set([...DISCOVERY_RELAYS, ...relays])].slice(0, 8);

  // Build time windows based on selected period
  let timeWindows: { since?: number; until?: number }[];

  if (timePeriod === "all") {
    // Deep crawl across all time — multiple windows for broader coverage
    timeWindows = [
      { since: now - 7 * day },                           // last week
      { since: now - 30 * day, until: now - 7 * day },    // 1-4 weeks ago
      { since: now - 90 * day, until: now - 30 * day },   // 1-3 months ago
      { since: now - 180 * day, until: now - 90 * day },  // 3-6 months ago
      { since: now - 365 * day, until: now - 180 * day }, // 6-12 months ago
      { until: now - 365 * day },                          // older than 1 year
    ];
  } else {
    const sincePeriod = (() => {
      switch (timePeriod) {
        case "today": return now - day;
        case "week": return now - 7 * day;
        case "month": return now - 30 * day;
        case "year": return now - 365 * day;
      }
    })();
    timeWindows = [{ since: sincePeriod }];
  }

  // Query each time window across relays in parallel
  const promises = timeWindows.flatMap(window =>
    queryRelays.map(async relay => {
      try {
        const filter: Filter = {
          kinds: ALL_VIDEO_KINDS,
          limit: Math.ceil(limit / timeWindows.length),
          ...window,
        };
        const events = await Promise.race([
          pool.querySync([relay], filter),
          new Promise<Event[]>((_, reject) => setTimeout(() => reject(new Error("timeout")), 10000)),
        ]);
        addEvents(events);
      } catch {}
    })
  );

  await Promise.allSettled(promises);

  // Parse all events into videos
  const parsed = allEvents
    .map(parseVideoEvent)
    .filter((v): v is ParsedVideo => v !== null);

  const deduped = dedupeVideos(parsed);

  if (deduped.length === 0) return [];

  // Fetch real engagement scores
  const scores = await fetchEngagementScores(deduped.map(v => v.id));

  // Filter ratioed content and apply scores
  const nowSecs = Date.now() / 1000;
  const filtered = deduped.filter(v => {
    const engagement = scores.get(v.id);
    if (engagement?.ratioed) return false;
    if (engagement) {
      v.zapCount = engagement.zapAmount;
    }
    return true;
  });

  if (sortBy === "zaps") {
    // Most Zapped: pure zap amount ranking
    filtered.sort((a, b) => {
      const zapA = scores.get(a.id)?.zapAmount || 0;
      const zapB = scores.get(b.id)?.zapAmount || 0;
      if (zapA !== zapB) return zapB - zapA;
      // Tiebreak by zap count
      const countA = scores.get(a.id)?.zapCount || 0;
      const countB = scores.get(b.id)?.zapCount || 0;
      return countB - countA;
    });
    // Filter out videos with zero zaps for Most Zapped
    return filtered.filter(v => {
      const e = scores.get(v.id);
      return e && (e.zapAmount > 0 || e.zapCount > 0);
    });
  }

  // Trending: composite score + time decay
  filtered.sort((a, b) => {
    const scoreA = scores.get(a.id)?.score || 0;
    const scoreB = scores.get(b.id)?.score || 0;

    // Time decay: boost recent content (168 hours = 1 week)
    const ageHoursA = (nowSecs - a.publishedAt) / 3600;
    const ageHoursB = (nowSecs - b.publishedAt) / 3600;
    const decayA = Math.max(0, 168 - ageHoursA) / 168;
    const decayB = Math.max(0, 168 - ageHoursB) / 168;

    const finalA = scoreA + decayA * 10;
    const finalB = scoreB + decayB * 10;
    return finalB - finalA;
  });

  return filtered;
}

function dedupeVideos(videos: ParsedVideo[]): ParsedVideo[] {
  const seen = new Set<string>();
  return videos.filter((v) => {
    if (seen.has(v.id)) return false;
    seen.add(v.id);
    return true;
  });
}
