import { useState, useEffect, useCallback, useMemo } from "react";
import { DEFAULT_RELAYS } from "@/lib/nostr";

const STORAGE_KEY = "videorelay_relays";
const OLD_STORAGE_KEY = "nostrtube_relays";
const RELAY_VERSION_KEY = "videorelay_relay_version";
const CURRENT_RELAY_VERSION = 3; // bump this when DEFAULT_RELAYS changes

export interface RelayEntry {
  url: string;
  enabled: boolean;
}

function loadRelays(): RelayEntry[] {
  try {
    // Migrate from old key if it exists
    const old = localStorage.getItem(OLD_STORAGE_KEY);
    if (old) {
      localStorage.removeItem(OLD_STORAGE_KEY);
    }

    // Check if defaults have been updated since last visit
    const savedVersion = parseInt(localStorage.getItem(RELAY_VERSION_KEY) || "0");
    if (savedVersion < CURRENT_RELAY_VERSION) {
      // Merge: keep any user-added relays, add new defaults, remove stale defaults
      const stored: RelayEntry[] = (() => {
        try {
          const raw = localStorage.getItem(STORAGE_KEY) || old;
          return raw ? JSON.parse(raw) : [];
        } catch { return []; }
      })();

      const defaultUrls = new Set(DEFAULT_RELAYS);
      const newDefaults = DEFAULT_RELAYS.map((url) => {
        const existing = stored.find((r) => r.url === url);
        return existing || { url, enabled: true };
      });

      // Keep user-added relays (not in any known default set)
      const userAdded = stored.filter((r) => !defaultUrls.has(r.url) && !isStaleRelay(r.url));
      const merged = [...newDefaults, ...userAdded];

      localStorage.setItem(RELAY_VERSION_KEY, CURRENT_RELAY_VERSION.toString());
      saveRelays(merged);
      return merged;
    }

    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return JSON.parse(stored);
  } catch {}
  localStorage.setItem(RELAY_VERSION_KEY, CURRENT_RELAY_VERSION.toString());
  return DEFAULT_RELAYS.map((url) => ({ url, enabled: true }));
}

// Relays we've intentionally removed from defaults
function isStaleRelay(url: string): boolean {
  const stale = [
    "wss://nostr-pub.wellorder.net",
    "wss://nostr.fmt.wiz.biz",
    "wss://nostr.mom",
    "wss://relay.mostr.pub",
    "wss://relay.nostrati.com",
    "wss://relay.noswhere.com",
  ];
  return stale.includes(url);
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
