import { useState, useEffect, useCallback } from "react";
import { getPool, DEFAULT_RELAYS } from "@/lib/nostr";

// Cache profiles in memory
const profileCache = new Map<string, NostrProfileData>();

export interface NostrProfileData {
  pubkey: string;
  name?: string;
  displayName?: string;
  picture?: string;
  banner?: string;
  about?: string;
  nip05?: string;
  lud16?: string;
}

export function useNostrProfile(pubkey: string | null) {
  const [profile, setProfile] = useState<NostrProfileData | null>(
    pubkey ? profileCache.get(pubkey) ?? null : null
  );
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!pubkey) return;

    if (profileCache.has(pubkey)) {
      setProfile(profileCache.get(pubkey)!);
      return;
    }

    setLoading(true);
    const pool = getPool();

    pool
      .querySync(DEFAULT_RELAYS, {
        kinds: [0],
        authors: [pubkey],
        limit: 1,
      })
      .then((events) => {
        if (events.length > 0) {
          try {
            const metadata = JSON.parse(events[0].content);
            const p: NostrProfileData = {
              pubkey,
              name: metadata.name,
              displayName: metadata.display_name || metadata.displayName,
              picture: metadata.picture,
              banner: metadata.banner,
              about: metadata.about,
              nip05: metadata.nip05,
              lud16: metadata.lud16,
            };
            profileCache.set(pubkey, p);
            setProfile(p);
          } catch {
            profileCache.set(pubkey, { pubkey });
            setProfile({ pubkey });
          }
        } else {
          profileCache.set(pubkey, { pubkey });
          setProfile({ pubkey });
        }
      })
      .catch(() => {
        setProfile({ pubkey });
      })
      .finally(() => setLoading(false));
  }, [pubkey]);

  return { profile, loading };
}

// Batch fetch profiles for multiple pubkeys
export function useBatchProfiles(pubkeys: string[]) {
  const [profiles, setProfiles] = useState<Map<string, NostrProfileData>>(new Map());
  const [loading, setLoading] = useState(false);

  const fetchProfiles = useCallback(async (keys: string[]) => {
    const uncached = keys.filter((k) => !profileCache.has(k));
    if (uncached.length === 0) {
      const map = new Map<string, NostrProfileData>();
      keys.forEach((k) => {
        if (profileCache.has(k)) map.set(k, profileCache.get(k)!);
      });
      setProfiles(map);
      return;
    }

    setLoading(true);
    try {
      const pool = getPool();
      const events = await pool.querySync(DEFAULT_RELAYS, {
        kinds: [0],
        authors: uncached,
      });

      for (const event of events) {
        try {
          const metadata = JSON.parse(event.content);
          const p: NostrProfileData = {
            pubkey: event.pubkey,
            name: metadata.name,
            displayName: metadata.display_name || metadata.displayName,
            picture: metadata.picture,
            banner: metadata.banner,
            about: metadata.about,
            nip05: metadata.nip05,
            lud16: metadata.lud16,
          };
          profileCache.set(event.pubkey, p);
        } catch {}
      }

      // Set fallback for any we didn't find
      uncached.forEach((k) => {
        if (!profileCache.has(k)) {
          profileCache.set(k, { pubkey: k });
        }
      });

      const map = new Map<string, NostrProfileData>();
      keys.forEach((k) => {
        if (profileCache.has(k)) map.set(k, profileCache.get(k)!);
      });
      setProfiles(map);
    } catch {
      // Fallbacks
      const map = new Map<string, NostrProfileData>();
      keys.forEach((k) => map.set(k, profileCache.get(k) || { pubkey: k }));
      setProfiles(map);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (pubkeys.length > 0) {
      fetchProfiles(pubkeys);
    }
  }, [pubkeys.join(",")]);

  return { profiles, loading };
}
