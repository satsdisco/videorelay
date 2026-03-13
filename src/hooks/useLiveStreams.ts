import { useState, useEffect, useCallback } from "react";
import { getPool, DEFAULT_RELAYS } from "@/lib/nostr";
import type { Event } from "nostr-tools";

export const LIVE_EVENT_KIND = 30311;
export const LIVE_CHAT_KIND = 1311;

export interface LiveStream {
  id: string;
  pubkey: string;
  title: string;
  summary: string;
  streamingUrl: string;
  thumbnailUrl: string;
  status: "live" | "ended";
  dTag: string;
  starts: number;
  viewers: number;
  tags: string[];
}

function parseLiveEvent(event: Event): LiveStream | null {
  const getTag = (name: string) => event.tags.find(t => t[0] === name)?.[1];
  const getAllTags = (name: string) => event.tags.filter(t => t[0] === name).map(t => t[1]);

  const status = getTag("status");
  if (status !== "live") return null;

  const streamingUrl = getTag("streaming") || getTag("recording") || "";
  if (!streamingUrl) return null;

  return {
    id: event.id,
    pubkey: event.pubkey,
    title: getTag("title") || "Untitled Stream",
    summary: getTag("summary") || event.content || "",
    streamingUrl,
    thumbnailUrl: getTag("image") || getTag("thumb") || "",
    status: "live",
    dTag: getTag("d") || "",
    starts: parseInt(getTag("starts") || "0") || event.created_at,
    viewers: 0,
    tags: getAllTags("t"),
  };
}

export function useLiveStreams(relays: string[] = DEFAULT_RELAYS) {
  const [streams, setStreams] = useState<LiveStream[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchStreams = useCallback(async () => {
    setLoading(true);
    try {
      const pool = getPool();
      const events = await pool.querySync(relays.slice(0, 5), {
        kinds: [LIVE_EVENT_KIND],
        limit: 50,
      });

      const parsed = events
        .map(parseLiveEvent)
        .filter((s): s is LiveStream => s !== null);

      // Dedupe by d-tag (keep most recent)
      const byDTag = new Map<string, LiveStream>();
      for (const stream of parsed) {
        const key = `${stream.pubkey}:${stream.dTag}`;
        const existing = byDTag.get(key);
        if (!existing || stream.starts > existing.starts) {
          byDTag.set(key, stream);
        }
      }

      setStreams(Array.from(byDTag.values()));
    } catch (err) {
      console.error("Failed to fetch live streams:", err);
    } finally {
      setLoading(false);
    }
  }, [relays]);

  useEffect(() => {
    fetchStreams();
  }, [fetchStreams]);

  return { streams, loading, refetch: fetchStreams };
}

export interface ChatMessage {
  id: string;
  pubkey: string;
  content: string;
  createdAt: number;
}

export function useLiveChat(streamId: string | null, relays: string[] = DEFAULT_RELAYS) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    if (!streamId) return;

    const pool = getPool();
    const sub = pool.subscribeMany(relays.slice(0, 3), [
      {
        kinds: [LIVE_CHAT_KIND],
        "#a": [`${LIVE_EVENT_KIND}:${streamId}`],
        limit: 100,
      },
    ], {
      onevent(event) {
        setMessages(prev => {
          if (prev.some(m => m.id === event.id)) return prev;
          const msg: ChatMessage = {
            id: event.id,
            pubkey: event.pubkey,
            content: event.content,
            createdAt: event.created_at,
          };
          return [...prev, msg].sort((a, b) => a.createdAt - b.createdAt).slice(-200);
        });
      },
    });

    return () => { sub.close(); };
  }, [streamId, relays]);

  return { messages };
}
