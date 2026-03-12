import { useState, useEffect, useCallback } from "react";
import { getPool, DEFAULT_RELAYS } from "@/lib/nostr";
import { useNostrAuth } from "./useNostrAuth";
import type { Event } from "nostr-tools";

export function useNostrFollow(targetPubkey: string | null) {
  const { pubkey, isLoggedIn } = useNostrAuth();
  const [isFollowing, setIsFollowing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [contactList, setContactList] = useState<Event | null>(null);

  // Fetch current user's contact list (kind 3)
  useEffect(() => {
    if (!pubkey || !isLoggedIn) return;

    const fetch = async () => {
      try {
        const pool = getPool();
        const events = await pool.querySync(DEFAULT_RELAYS, {
          kinds: [3],
          authors: [pubkey],
          limit: 1,
        });

        if (events.length > 0) {
          const latest = events.sort((a, b) => b.created_at - a.created_at)[0];
          setContactList(latest);

          // Check if target is in contact list
          if (targetPubkey) {
            const follows = latest.tags.filter((t) => t[0] === "p").map((t) => t[1]);
            setIsFollowing(follows.includes(targetPubkey));
          }
        }
      } catch (err) {
        console.warn("Failed to fetch contact list:", err);
      }
    };
    fetch();
  }, [pubkey, isLoggedIn, targetPubkey]);

  const toggleFollow = useCallback(async () => {
    if (!pubkey || !targetPubkey || !window.nostr || loading) return;

    setLoading(true);
    try {
      let newTags: string[][] = [];

      if (contactList) {
        newTags = [...contactList.tags];
      }

      if (isFollowing) {
        // Remove
        newTags = newTags.filter((t) => !(t[0] === "p" && t[1] === targetPubkey));
      } else {
        // Add
        newTags.push(["p", targetPubkey]);
      }

      const event = {
        kind: 3,
        content: contactList?.content || "",
        tags: newTags,
        created_at: Math.floor(Date.now() / 1000),
      };

      const signedEvent = await (window.nostr as any).signEvent(event);
      const pool = getPool();
      const pubs = pool.publish(DEFAULT_RELAYS, signedEvent);
      await Promise.allSettled(pubs);

      setIsFollowing(!isFollowing);
      setContactList(signedEvent);
    } catch (err) {
      console.error("Follow/unfollow failed:", err);
    } finally {
      setLoading(false);
    }
  }, [pubkey, targetPubkey, isFollowing, contactList, loading]);

  return { isFollowing, loading, toggleFollow };
}
