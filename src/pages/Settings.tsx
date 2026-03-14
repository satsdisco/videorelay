import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Globe,
  Palette,
  Video,
  Shield,
  Plus,
  X,
  RotateCcw,
  Wifi,
  WifiOff,
  Eye,
  EyeOff,
  Zap,
  Monitor,
} from "lucide-react";
import { useRelayStore } from "@/hooks/useRelayStore";
import { DEFAULT_RELAYS } from "@/lib/nostr";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { getCuratedCreators, addCuratedCreator, removeCuratedCreator, npubToHex, type CuratedCreator } from "@/lib/curatedCreators";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { Star, UserPlus as UserPlusIcon } from "lucide-react";

// ─── Preferences persisted in localStorage ───
const PREFS_KEY = "videorelay_prefs";

interface Preferences {
  autoplay: boolean;
  muteAutoplay: boolean;
  defaultQuality: "auto" | "high" | "medium" | "low";
  nsfwBlur: boolean;
  showZapAmounts: boolean;
  compactMode: boolean;
}

const defaultPrefs: Preferences = {
  autoplay: true,
  muteAutoplay: true,
  defaultQuality: "auto",
  nsfwBlur: true,
  showZapAmounts: true,
  compactMode: false,
};

function loadPrefs(): Preferences {
  try {
    const s = localStorage.getItem(PREFS_KEY);
    if (s) return { ...defaultPrefs, ...JSON.parse(s) };
  } catch {}
  return defaultPrefs;
}

function savePrefs(p: Preferences) {
  localStorage.setItem(PREFS_KEY, JSON.stringify(p));
}

// ─── Relay Tab ───
const RelaySettings = () => {
  const { relays, addRelay, removeRelay, toggleRelay, resetToDefaults, activeRelays } = useRelayStore();
  const [newRelay, setNewRelay] = useState("");
  const [error, setError] = useState("");

  const handleAdd = () => {
    setError("");
    const url = newRelay.trim();
    if (!url) return;
    if (!url.startsWith("wss://") && !url.startsWith("ws://")) {
      setError("Must start with wss:// or ws://");
      return;
    }
    if (relays.some((r) => r.url === url.replace(/\/+$/, ""))) {
      setError("Already added");
      return;
    }
    addRelay(url);
    setNewRelay("");
  };

  return (
    <div className="space-y-6">
      {/* Stats */}
      <div className="flex items-center gap-4 text-sm">
        <span className="px-3 py-1 rounded-full bg-primary/10 text-primary font-medium">
          {activeRelays.length} active
        </span>
        <span className="text-muted-foreground">{relays.length} total</span>
      </div>

      {/* Add relay */}
      <div>
        <label className="text-sm font-medium text-foreground mb-2 block">Add Relay</label>
        <div className="flex gap-2">
          <input
            type="text"
            value={newRelay}
            onChange={(e) => { setNewRelay(e.target.value); setError(""); }}
            onKeyDown={(e) => e.key === "Enter" && handleAdd()}
            placeholder="wss://relay.example.com"
            className="flex-1 bg-secondary border border-border rounded-lg px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground outline-none focus:border-primary/50 transition-colors"
          />
          <button
            onClick={handleAdd}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:opacity-90 transition-opacity"
          >
            <Plus className="w-4 h-4" />
            Add
          </button>
        </div>
        {error && <p className="text-xs text-destructive mt-1.5">{error}</p>}
      </div>

      {/* Relay list */}
      <div className="space-y-1">
        {relays.map((relay) => {
          const isDefault = DEFAULT_RELAYS.includes(relay.url);
          return (
            <div
              key={relay.url}
              className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-secondary/50 transition-colors group"
            >
              <button
                onClick={() => toggleRelay(relay.url)}
                className={`shrink-0 transition-colors ${relay.enabled ? "text-green-500" : "text-muted-foreground"}`}
              >
                {relay.enabled ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
              </button>
              <p className={`flex-1 text-sm truncate ${relay.enabled ? "text-foreground" : "text-muted-foreground line-through"}`}>
                {relay.url}
              </p>
              {!isDefault && (
                <button
                  onClick={() => removeRelay(relay.url)}
                  className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-all"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          );
        })}
      </div>

      <button
        onClick={resetToDefaults}
        className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
      >
        <RotateCcw className="w-3.5 h-3.5" />
        Reset to defaults
      </button>
    </div>
  );
};

// ─── Playback Tab ───
const PlaybackSettings = ({ prefs, setPrefs }: { prefs: Preferences; setPrefs: (p: Preferences) => void }) => {
  const update = (partial: Partial<Preferences>) => {
    const next = { ...prefs, ...partial };
    setPrefs(next);
    savePrefs(next);
  };

  return (
    <div className="space-y-6">
      <SettingRow
        icon={<Video className="w-4 h-4" />}
        title="Autoplay"
        description="Automatically play the next video"
      >
        <Switch checked={prefs.autoplay} onCheckedChange={(v) => update({ autoplay: v })} />
      </SettingRow>

      <SettingRow
        icon={<Monitor className="w-4 h-4" />}
        title="Mute on autoplay"
        description="Start autoplayed videos muted"
      >
        <Switch checked={prefs.muteAutoplay} onCheckedChange={(v) => update({ muteAutoplay: v })} />
      </SettingRow>

      <SettingRow
        icon={<Video className="w-4 h-4" />}
        title="Default quality"
        description="Preferred video quality when available"
      >
        <select
          value={prefs.defaultQuality}
          onChange={(e) => update({ defaultQuality: e.target.value as Preferences["defaultQuality"] })}
          className="bg-secondary border border-border rounded-lg px-3 py-1.5 text-sm text-foreground outline-none focus:border-primary/50"
        >
          <option value="auto">Auto</option>
          <option value="high">High</option>
          <option value="medium">Medium</option>
          <option value="low">Low</option>
        </select>
      </SettingRow>
    </div>
  );
};

// ─── Content Tab ───
const ContentSettings = ({ prefs, setPrefs }: { prefs: Preferences; setPrefs: (p: Preferences) => void }) => {
  const update = (partial: Partial<Preferences>) => {
    const next = { ...prefs, ...partial };
    setPrefs(next);
    savePrefs(next);
  };

  return (
    <div className="space-y-6">
      <SettingRow
        icon={<Shield className="w-4 h-4" />}
        title="Blur sensitive content"
        description="Blur thumbnails marked as NSFW (content-warning tag)"
      >
        <Switch checked={prefs.nsfwBlur} onCheckedChange={(v) => update({ nsfwBlur: v })} />
      </SettingRow>

      <SettingRow
        icon={<Zap className="w-4 h-4" />}
        title="Show zap amounts"
        description="Display zap amounts on videos"
      >
        <Switch checked={prefs.showZapAmounts} onCheckedChange={(v) => update({ showZapAmounts: v })} />
      </SettingRow>

      <SettingRow
        icon={<Palette className="w-4 h-4" />}
        title="Compact mode"
        description="Show smaller video cards to fit more on screen"
      >
        <Switch checked={prefs.compactMode} onCheckedChange={(v) => update({ compactMode: v })} />
      </SettingRow>
    </div>
  );
};

// ─── Helper ───
const SettingRow = ({
  icon,
  title,
  description,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  children: React.ReactNode;
}) => (
  <div className="flex items-start justify-between gap-4">
    <div className="flex items-start gap-3">
      <div className="mt-0.5 text-muted-foreground">{icon}</div>
      <div>
        <p className="text-sm font-medium text-foreground">{title}</p>
        <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
      </div>
    </div>
    <div className="shrink-0">{children}</div>
  </div>
);

// ─── Curated Creator Row ───
const CreatorRow = ({ creator, onRemove }: { creator: CuratedCreator; onRemove: () => void }) => {
  const { profile } = useNostrProfile(creator.pubkey);
  const displayName = profile?.displayName || profile?.name || creator.label || creator.pubkey.slice(0, 12) + "...";

  return (
    <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary/50">
      {profile?.picture ? (
        <img src={profile.picture} alt={displayName} className="w-10 h-10 rounded-full object-cover shrink-0" />
      ) : (
        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-sm font-bold text-primary-foreground shrink-0">
          {displayName[0]?.toUpperCase() || "?"}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-foreground truncate">{displayName}</p>
        {profile?.nip05 && <p className="text-xs text-primary truncate">{profile.nip05}</p>}
        <p className="text-xs text-muted-foreground truncate font-mono">{creator.pubkey.slice(0, 16)}...</p>
      </div>
      <button
        onClick={onRemove}
        className="p-1.5 rounded-lg hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-colors shrink-0"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

// ─── Curated Creators Admin ───
const CuratedCreatorsSettings = () => {
  const [creators, setCreators] = useState<CuratedCreator[]>(getCuratedCreators);
  const [input, setInput] = useState("");
  const [error, setError] = useState("");

  const handleAdd = () => {
    setError("");
    const val = input.trim();
    if (!val) return;

    let hex: string | null = null;
    if (val.startsWith("npub1")) {
      hex = npubToHex(val);
      if (!hex || hex.length !== 64) {
        setError("Invalid npub");
        return;
      }
    } else if (/^[0-9a-f]{64}$/i.test(val)) {
      hex = val.toLowerCase();
    } else {
      setError("Enter an npub or hex pubkey");
      return;
    }

    if (creators.some(c => c.pubkey === hex)) {
      setError("Already added");
      return;
    }

    addCuratedCreator(hex);
    setCreators(getCuratedCreators());
    setInput("");
  };

  const handleRemove = (pubkey: string) => {
    removeCuratedCreator(pubkey);
    setCreators(getCuratedCreators());
  };

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-base font-semibold text-foreground mb-1">Curated Creators</h3>
        <p className="text-sm text-muted-foreground">
          Pin creators whose videos get featured on the home page. Their latest videos will appear in a "Featured" section at the top of the feed.
        </p>
      </div>

      {/* Add creator */}
      <div className="flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleAdd()}
          placeholder="npub1... or hex pubkey"
          className="flex-1 px-3 py-2 bg-secondary rounded-lg text-sm text-foreground placeholder:text-muted-foreground outline-none focus:ring-2 focus:ring-primary/50 font-mono"
        />
        <button
          onClick={handleAdd}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors flex items-center gap-1.5"
        >
          <UserPlusIcon className="w-4 h-4" />
          Add
        </button>
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}

      {/* Creator list */}
      {creators.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          <Star className="w-8 h-8 mx-auto mb-2 opacity-50" />
          <p className="text-sm">No curated creators yet</p>
          <p className="text-xs mt-1">Add npubs of creators with great video content</p>
        </div>
      ) : (
        <div className="space-y-2">
          {creators.map((c) => (
            <CreatorRow key={c.pubkey} creator={c} onRemove={() => handleRemove(c.pubkey)} />
          ))}
        </div>
      )}
    </div>
  );
};

// ─── Main ───
const Settings = () => {
  const navigate = useNavigate();
  const { isLoggedIn, pubkey, profile } = useNostrAuth();
  const [prefs, setPrefs] = useState<Preferences>(loadPrefs);

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <div className="sticky top-0 z-40 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="max-w-2xl mx-auto flex items-center gap-3 h-14 px-4">
          <button onClick={() => navigate(-1)} className="p-2 -ml-2 rounded-lg hover:bg-secondary transition-colors">
            <ArrowLeft className="w-5 h-5 text-foreground" />
          </button>
          <h1 className="text-lg font-bold text-foreground">Settings</h1>
        </div>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-6">
        {/* Account summary */}
        {isLoggedIn && (
          <div className="flex items-center gap-3 p-4 mb-6 rounded-xl bg-secondary/50 border border-border">
            <div className="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-br from-primary to-accent flex items-center justify-center text-sm font-bold text-primary-foreground shrink-0">
              {profile?.picture ? (
                <img src={profile.picture} alt="" className="w-full h-full object-cover" />
              ) : (
                (profile?.name || pubkey)?.[0]?.toUpperCase() || "?"
              )}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-foreground truncate">
                {profile?.displayName || profile?.name || pubkey?.slice(0, 12)}
              </p>
              {profile?.nip05 && <p className="text-xs text-primary truncate">{profile.nip05}</p>}
            </div>
          </div>
        )}

        <Tabs defaultValue="relays" className="w-full">
          <TabsList className="w-full bg-secondary/50 rounded-xl p-1 h-auto flex">
            <TabsTrigger value="relays" className="flex-1 gap-1.5 py-2 rounded-lg data-[state=active]:bg-background">
              <Globe className="w-4 h-4" />
              <span className="hidden sm:inline">Relays</span>
            </TabsTrigger>
            <TabsTrigger value="playback" className="flex-1 gap-1.5 py-2 rounded-lg data-[state=active]:bg-background">
              <Video className="w-4 h-4" />
              <span className="hidden sm:inline">Playback</span>
            </TabsTrigger>
            <TabsTrigger value="content" className="flex-1 gap-1.5 py-2 rounded-lg data-[state=active]:bg-background">
              <Shield className="w-4 h-4" />
              <span className="hidden sm:inline">Content</span>
            </TabsTrigger>
            <TabsTrigger value="admin" className="flex-1 gap-1.5 py-2 rounded-lg data-[state=active]:bg-background">
              <Star className="w-4 h-4" />
              <span className="hidden sm:inline">Curate</span>
            </TabsTrigger>
          </TabsList>

          <div className="mt-6">
            <TabsContent value="relays">
              <RelaySettings />
            </TabsContent>
            <TabsContent value="playback">
              <PlaybackSettings prefs={prefs} setPrefs={setPrefs} />
            </TabsContent>
            <TabsContent value="content">
              <ContentSettings prefs={prefs} setPrefs={setPrefs} />
            </TabsContent>
            <TabsContent value="admin">
              <CuratedCreatorsSettings />
            </TabsContent>
          </div>
        </Tabs>

        <div className="mt-10 pt-6 border-t border-border text-center">
          <p className="text-xs text-muted-foreground">
            VideoRelay · Built on the Nostr protocol · Censorship-resistant · Zap-powered
          </p>
        </div>
      </div>
    </div>
  );
};

export default Settings;
