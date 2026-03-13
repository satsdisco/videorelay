import { getPool, parseVideoEvent, type ParsedVideo, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND } from "./nostr";
import type { Filter, Event } from "nostr-tools";

const VIDEO_KINDS = [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND];

// Aggregator relays that index the broadest content
const DISCOVERY_RELAYS = [
  "wss://relay.nostr.band",
  "wss://relay.damus.io",
  "wss://relay.primal.net",
];

// Cache engagement scores in memory and localStorage
const ENGAGEMENT_CACHE_KEY = "videorelay_engagement";
const ENGAGEMENT_CACHE_TTL = 1000 * 60 * 15; // 15 min

interface EngagementScore {
  reactions: number;
  replies: number;
  reposts: number;
  zapAmount: number;
  zapCount: number;
  score: number; // composite
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

  // Initialize all with zero scores
  for (const id of videoIds) {
    if (engagementCache.has(id)) {
      scores.set(id, engagementCache.get(id)!);
    } else {
      scores.set(id, { reactions: 0, replies: 0, reposts: 0, zapAmount: 0, zapCount: 0, score: 0 });
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
      // Fetch reactions (kind 7), reposts (kind 6, 16), and zaps (kind 9735) in parallel
      const [reactions, zaps] = await Promise.allSettled([
        pool.querySync(DISCOVERY_RELAYS.slice(0, 2), {
          kinds: [7, 6, 16],
          "#e": chunk,
          limit: 1000,
        }),
        pool.querySync(DISCOVERY_RELAYS.slice(0, 2), {
          kinds: [9735],
          "#e": chunk,
          limit: 500,
        }),
      ]);

      // Count reactions and reposts
      if (reactions.status === "fulfilled") {
        for (const event of reactions.value) {
          const eTag = event.tags.find(t => t[0] === "e");
          if (!eTag) continue;
          const videoId = eTag[1];
          const entry = scores.get(videoId);
          if (!entry) continue;

          if (event.kind === 7) entry.reactions++;
          else if (event.kind === 6 || event.kind === 16) entry.reposts++;
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
    // Weighted score: zap_amount is king, reactions and reposts matter too
    entry.score = (entry.zapAmount * 1) + (entry.zapCount * 100) + (entry.reactions * 10) + (entry.reposts * 50);
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
  } = {}
): Promise<ParsedVideo[]> {
  const { timePeriod = "all", limit = 300 } = options;
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
          kinds: VIDEO_KINDS,
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

  // Apply scores to videos
  for (const video of deduped) {
    const engagement = scores.get(video.id);
    if (engagement) {
      video.zapCount = engagement.zapAmount; // Use actual sats amount
    }
  }

  // Sort by composite engagement score
  deduped.sort((a, b) => {
    const scoreA = scores.get(a.id)?.score || 0;
    const scoreB = scores.get(b.id)?.score || 0;
    return scoreB - scoreA;
  });

  return deduped;
}

function dedupeVideos(videos: ParsedVideo[]): ParsedVideo[] {
  const seen = new Set<string>();
  return videos.filter((v) => {
    if (seen.has(v.id)) return false;
    seen.add(v.id);
    return true;
  });
}
