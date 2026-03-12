import { useState, useEffect, useCallback } from "react";

const STORAGE_KEY = "nostrtube_relays";

// Curated defaults — video-heavy and reliable relays
export const DEFAULT_RELAYS = [
  "wss://relay.damus.io",
  "wss://relay.nostr.band",
  "wss://nos.lol",
  "wss://relay.snort.social",
  "wss://nostr.wine",
  "wss://relay.primal.net",
  "wss://nostr-pub.wellorder.net",
  "wss://relay.nostr.bg",
  "wss://nostr.fmt.wiz.biz",
];

export interface RelayEntry {
  url: string;
  enabled: boolean;
}

function loadRelays(): RelayEntry[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return JSON.parse(stored);
  } catch {}
  return DEFAULT_RELAYS.map((url) => ({ url, enabled: true }));
}

function saveRelays(relays: RelayEntry[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(relays));
}

export function useRelayStore() {
  const [relays, setRelays] = useState<RelayEntry[]>(loadRelays);

  useEffect(() => {
    saveRelays(relays);
  }, [relays]);

  const activeRelays = relays.filter((r) => r.enabled).map((r) => r.url);

  const addRelay = useCallback((url: string) => {
    const normalized = url.trim().replace(/\/+$/, "");
    if (!normalized.startsWith("wss://") && !normalized.startsWith("ws://")) return false;
    setRelays((prev) => {
      if (prev.some((r) => r.url === normalized)) return prev;
      return [...prev, { url: normalized, enabled: true }];
    });
    return true;
  }, []);

  const removeRelay = useCallback((url: string) => {
    setRelays((prev) => prev.filter((r) => r.url !== url));
  }, []);

  const toggleRelay = useCallback((url: string) => {
    setRelays((prev) =>
      prev.map((r) => (r.url === url ? { ...r, enabled: !r.enabled } : r))
    );
  }, []);

  const resetToDefaults = useCallback(() => {
    const defaults = DEFAULT_RELAYS.map((url) => ({ url, enabled: true }));
    setRelays(defaults);
  }, []);

  return { relays, activeRelays, addRelay, removeRelay, toggleRelay, resetToDefaults };
}
