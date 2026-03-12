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

export function NostrAuthProvider({ children }: { children: ReactNode }) {
  const [pubkey, setPubkey] = useState<string | null>(null);
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

  const login = useCallback(async () => {
    if (!window.nostr) {
      throw new Error("No Nostr extension found. Install Alby or nos2x.");
    }

    try {
      const pk = await window.nostr.getPublicKey();
      setPubkey(pk);
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

// Type augmentation for NIP-07
declare global {
  interface Window {
    nostr?: {
      getPublicKey: () => Promise<string>;
      signEvent: (event: object) => Promise<object>;
      getRelays?: () => Promise<Record<string, { read: boolean; write: boolean }>>;
      nip04?: {
        encrypt: (pubkey: string, plaintext: string) => Promise<string>;
        decrypt: (pubkey: string, ciphertext: string) => Promise<string>;
      };
    };
  }
}
