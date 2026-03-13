import { useState, useEffect, useCallback, useMemo } from "react";
import { DEFAULT_RELAYS } from "@/lib/nostr";

const STORAGE_KEY = "videorelay_relays";
const OLD_STORAGE_KEY = "nostrtube_relays";

export interface RelayEntry {
  url: string;
  enabled: boolean;
}

function loadRelays(): RelayEntry[] {
  try {
    // Migrate from old key if it exists
    const old = localStorage.getItem(OLD_STORAGE_KEY);
    if (old) {
      localStorage.setItem(STORAGE_KEY, old);
      localStorage.removeItem(OLD_STORAGE_KEY);
      return JSON.parse(old);
    }
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

  const activeRelays = useMemo(
    () => relays.filter((r) => r.enabled).map((r) => r.url),
    [relays]
  );

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
