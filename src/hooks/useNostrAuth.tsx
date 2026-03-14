import { useState, useCallback, createContext, useContext, type ReactNode, useEffect } from "react";

interface NostrProfile {
  pubkey: string;
  name?: string;
  displayName?: string;
  picture?: string;
  about?: string;
  nip05?: string;
}

interface NostrAuthState {
  isLoggedIn: boolean;
  pubkey: string | null;
  profile: NostrProfile | null;
  isExtensionAvailable: boolean;
  login: () => Promise<void>;
  logout: () => void;
}

const NostrAuthContext = createContext<NostrAuthState | null>(null);

const AUTH_KEY = "videorelay_auth";

function persistAuth(pk: string | null) {
  if (pk) {
    localStorage.setItem(AUTH_KEY, pk);
  } else {
    localStorage.removeItem(AUTH_KEY);
  }
}

function loadPersistedAuth(): string | null {
  try { return localStorage.getItem(AUTH_KEY); } catch { return null; }
}

export function NostrAuthProvider({ children }: { children: ReactNode }) {
  const [pubkey, setPubkey] = useState<string | null>(loadPersistedAuth);
  const [profile, setProfile] = useState<NostrProfile | null>(null);
  const [isExtensionAvailable, setIsExtensionAvailable] = useState(false);

  useEffect(() => {
    // Check for NIP-07 extension
    const check = () => {
      setIsExtensionAvailable(!!window.nostr);
    };
    check();
    // Some extensions load async
    const timer = setTimeout(check, 1000);
    return () => clearTimeout(timer);
  }, []);

  // Restore session on mount if we have a persisted pubkey
  useEffect(() => {
    const savedPk = loadPersistedAuth();
    if (savedPk && !profile) {
      setPubkey(savedPk);
      setProfile({ pubkey: savedPk });
      // Fetch full profile in background
      (async () => {
        try {
          const { getPool, DEFAULT_RELAYS } = await import("@/lib/nostr");
          const pool = getPool();
          const events = await pool.querySync(DEFAULT_RELAYS, {
            kinds: [0],
            authors: [savedPk],
            limit: 1,
          });
          if (events.length > 0) {
            try {
              const metadata = JSON.parse(events[0].content);
              setProfile({
                pubkey: savedPk,
                name: metadata.name,
                displayName: metadata.display_name || metadata.displayName,
                picture: metadata.picture,
                about: metadata.about,
                nip05: metadata.nip05,
              });
            } catch {}
          }
        } catch {}
      })();
    }
  }, []);

  const login = useCallback(async () => {
    if (!window.nostr) {
      throw new Error("No Nostr extension found. Install Alby or nos2x.");
    }

    try {
      const pk = await window.nostr.getPublicKey();
      setPubkey(pk);
      persistAuth(pk);
      setProfile({ pubkey: pk });

      // Fetch profile (kind 0) from relays
      const { getPool, DEFAULT_RELAYS } = await import("@/lib/nostr");
      const pool = getPool();
      const events = await pool.querySync(DEFAULT_RELAYS, {
        kinds: [0],
        authors: [pk],
        limit: 1,
      });

      if (events.length > 0) {
        try {
          const metadata = JSON.parse(events[0].content);
          setProfile({
            pubkey: pk,
            name: metadata.name,
            displayName: metadata.display_name || metadata.displayName,
            picture: metadata.picture,
            about: metadata.about,
            nip05: metadata.nip05,
          });
        } catch {}
      }
    } catch (err) {
      console.error("Login failed:", err);
      throw err;
    }
  }, []);

  const logout = useCallback(() => {
    setPubkey(null);
    setProfile(null);
    persistAuth(null);
  }, []);

  return (
    <NostrAuthContext.Provider
      value={{
        isLoggedIn: !!pubkey,
        pubkey,
        profile,
        isExtensionAvailable,
        login,
        logout,
      }}
    >
      {children}
    </NostrAuthContext.Provider>
  );
}

export function useNostrAuth() {
  const ctx = useContext(NostrAuthContext);
  if (!ctx) throw new Error("useNostrAuth must be used within NostrAuthProvider");
  return ctx;
}

// NIP-07 types are in src/types/nostr.d.ts
