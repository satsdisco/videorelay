import { useState, useEffect } from "react";
import { getPool, DEFAULT_RELAYS } from "@/lib/nostr";
import { useNostrAuth } from "./useNostrAuth";

/**
 * Fetch the logged-in user's follow list (kind 3 contact list).
 * Returns an array of followed pubkeys.
 */
export function useFollowList() {
  const { pubkey, isLoggedIn } = useNostrAuth();
  const [followedPubkeys, setFollowedPubkeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!pubkey || !isLoggedIn) {
      setFollowedPubkeys([]);
      return;
    }

    setLoading(true);
    const pool = getPool();

    pool
      .querySync(DEFAULT_RELAYS, {
        kinds: [3],
        authors: [pubkey],
        limit: 1,
      })
      .then((events) => {
        if (events.length > 0) {
          // Get the most recent contact list
          const latest = events.sort((a, b) => b.created_at - a.created_at)[0];
          const follows = latest.tags
            .filter((t) => t[0] === "p")
            .map((t) => t[1]);
          setFollowedPubkeys(follows);
        } else {
          setFollowedPubkeys([]);
        }
      })
      .catch((err) => {
        console.warn("Failed to fetch follow list:", err);
        setFollowedPubkeys([]);
      })
      .finally(() => setLoading(false));
  }, [pubkey, isLoggedIn]);

  return { followedPubkeys, loading };
}
