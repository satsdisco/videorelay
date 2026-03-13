import { useCallback, useState } from "react";
import { getPool, DEFAULT_RELAYS } from "@/lib/nostr";
import { useNostrAuth } from "./useNostrAuth";
import { useRelayStore } from "./useRelayStore";
import type { Event } from "nostr-tools";

interface ZapTarget {
  eventId: string;
  recipientPubkey: string;
}

interface ZapState {
  loading: boolean;
  success: boolean;
  error: string | null;
  invoice: string | null; // bolt11 invoice for manual payment
}

/**
 * Parse a Lightning address (user@domain) or LNURL from a profile's lud16/lud06 field.
 * Returns the LNURL callback URL.
 */
async function getLnurlPayUrl(lud16?: string, lud06?: string): Promise<string | null> {
  // lud16 is a Lightning address like user@domain.com
  if (lud16) {
    const [name, domain] = lud16.split("@");
    if (name && domain) {
      return `https://${domain}/.well-known/lnurlp/${name}`;
    }
  }

  // lud06 is a bech32-encoded LNURL — decode it
  if (lud06) {
    try {
      // Simple bech32 decode for LNURL
      const { bech32 } = await import("@scure/base");
      const decoded = bech32.decode(lud06 as any, 2000);
      const bytes = bech32.fromWords(decoded.words);
      return new TextDecoder().decode(new Uint8Array(bytes));
    } catch {
      return null;
    }
  }

  return null;
}

export function useZap() {
  const { pubkey } = useNostrAuth();
  const { activeRelays } = useRelayStore();
  const [state, setState] = useState<ZapState>({
    loading: false,
    success: false,
    error: null,
    invoice: null,
  });

  const zap = useCallback(
    async (target: ZapTarget, amountSats: number) => {
      setState({ loading: true, success: false, error: null, invoice: null });

      try {
        if (!window.nostr) {
          throw new Error("No Nostr extension found. Install Alby or nos2x to sign zap requests.");
        }
        if (!pubkey) {
          throw new Error("Sign in first to send zaps.");
        }

        // 1. Fetch recipient's profile to get their Lightning address
        const pool = getPool();
        const profileEvents = await pool.querySync(DEFAULT_RELAYS, {
          kinds: [0],
          authors: [target.recipientPubkey],
          limit: 1,
        });

        if (profileEvents.length === 0) {
          throw new Error("Could not find recipient's profile on relays.");
        }

        let metadata: any;
        try {
          metadata = JSON.parse(profileEvents[0].content);
        } catch {
          throw new Error("Could not parse recipient's profile.");
        }

        const lnurlPayUrl = await getLnurlPayUrl(metadata.lud16, metadata.lud06);
        if (!lnurlPayUrl) {
          throw new Error(
            "This creator doesn't have a Lightning address set up."
          );
        }

        // 2. Fetch LNURL pay params
        const lnurlRes = await fetch(lnurlPayUrl);
        if (!lnurlRes.ok) {
          throw new Error("Failed to reach creator's Lightning provider.");
        }
        const lnurlData = await lnurlRes.json();

        const amountMsat = amountSats * 1000;
        if (lnurlData.minSendable && amountMsat < lnurlData.minSendable) {
          throw new Error(
            `Minimum zap is ${Math.ceil(lnurlData.minSendable / 1000)} sats.`
          );
        }
        if (lnurlData.maxSendable && amountMsat > lnurlData.maxSendable) {
          throw new Error(
            `Maximum zap is ${Math.floor(lnurlData.maxSendable / 1000)} sats.`
          );
        }

        // 3. Create NIP-57 zap request (kind 9734)
        const zapRequest = {
          kind: 9734,
          content: "",
          tags: [
            ["p", target.recipientPubkey],
            ["e", target.eventId],
            ["amount", amountMsat.toString()],
            ["relays", ...activeRelays.slice(0, 5)],
          ],
          created_at: Math.floor(Date.now() / 1000),
        };

        const signedZapRequest = await (window.nostr as any).signEvent(
          zapRequest
        );

        // 4. Request invoice from LNURL with zap request
        const callbackUrl = lnurlData.callback || lnurlPayUrl;
        const separator = callbackUrl.includes("?") ? "&" : "?";
        const invoiceUrl = `${callbackUrl}${separator}amount=${amountMsat}&nostr=${encodeURIComponent(JSON.stringify(signedZapRequest))}`;

        const invoiceRes = await fetch(invoiceUrl);
        if (!invoiceRes.ok) {
          throw new Error("Failed to get Lightning invoice.");
        }
        const invoiceData = await invoiceRes.json();

        if (!invoiceData.pr) {
          throw new Error(
            invoiceData.reason || "No invoice returned from provider."
          );
        }

        // 5. Try WebLN first, fall back to showing invoice
        const hasWebLN = !!(window as any).webln;
        if (hasWebLN) {
          try {
            await (window as any).webln.enable();
            await (window as any).webln.sendPayment(invoiceData.pr);
            setState({ loading: false, success: true, error: null, invoice: null });
          } catch (webLnErr: any) {
            // WebLN failed (rejected, not available) — show invoice instead
            setState({ loading: false, success: false, error: null, invoice: invoiceData.pr });
            return;
          }
        } else {
          // No WebLN — show invoice for manual payment
          setState({ loading: false, success: false, error: null, invoice: invoiceData.pr });
          return;
        }

        // Reset success after a few seconds
        setTimeout(() => {
          setState((s) => (s.success ? { ...s, success: false } : s));
        }, 3000);
      } catch (err: any) {
        console.error("Zap failed:", err);
        setState({
          loading: false,
          success: false,
          error: err?.message || "Zap failed",
          invoice: null,
        });

        // Clear error after a few seconds
        setTimeout(() => {
          setState((s) => (s.error ? { ...s, error: null } : s));
        }, 5000);
      }
    },
    [pubkey, activeRelays]
  );

  return { zap, ...state };
}
