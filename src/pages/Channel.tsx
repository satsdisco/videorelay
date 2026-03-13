import { useParams, useNavigate } from "react-router-dom";
import { useState, useMemo } from "react";
import { ArrowLeft, ExternalLink, Zap, Clock, TrendingUp } from "lucide-react";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useNostrVideos } from "@/hooks/useNostrVideos";
import { useRelayStore } from "@/hooks/useRelayStore";
import { shortenNpub } from "@/lib/nostr";
import VideoCard from "@/components/VideoCard";

const Channel = () => {
  const { pubkey } = useParams<{ pubkey: string }>();
  const navigate = useNavigate();
  const { activeRelays } = useRelayStore();
  const { profile, loading: profileLoading } = useNostrProfile(pubkey ?? null);
  const [sortBy, setSortBy] = useState<"recent" | "popular">("recent");

  const { videos, loading: videosLoading } = useNostrVideos({
    relays: activeRelays,
    authors: pubkey ? [pubkey] : undefined,
    limit: 200,
    sortBy,
  });

  const longForm = useMemo(() => videos.filter((v) => !v.isShort), [videos]);
  const displayName = profile?.displayName || profile?.name || (pubkey ? shortenNpub(pubkey) : "Unknown");
  const totalZaps = useMemo(() => videos.reduce((sum, v) => sum + v.zapCount, 0), [videos]);

  return (
    <div className="min-h-screen bg-background pb-16 md:pb-0">
      {/* Top bar */}
      <div className="sticky top-0 z-50 flex items-center gap-3 px-4 py-3 bg-background/95 backdrop-blur-sm border-b border-border">
        <button onClick={() => navigate("/")} className="p-2 rounded-lg hover:bg-secondary transition-colors">
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </button>
        <h1 className="text-sm font-medium text-foreground truncate">{displayName}'s Channel</h1>
      </div>

      {/* Banner / Profile header */}
      <div className="relative">
        <div className="h-32 bg-gradient-to-r from-primary/30 via-primary/10 to-accent/20" />
        <div className="max-w-5xl mx-auto px-6 -mt-12">
          <div className="flex items-end gap-5">
            {profile?.picture ? (
              <img src={profile.picture} alt={displayName} className="w-24 h-24 rounded-full object-cover border-4 border-background shadow-xl" />
            ) : (
              <div className="w-24 h-24 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-2xl font-bold text-primary-foreground border-4 border-background shadow-xl">
                {displayName[0]?.toUpperCase() || "?"}
              </div>
            )}
            <div className="pb-2">
              <h2 className="text-2xl font-bold text-foreground">{displayName}</h2>
              {profile?.nip05 && <p className="text-sm text-primary">{profile.nip05}</p>}
              <div className="flex items-center gap-4 mt-1 text-sm text-muted-foreground">
                <span>{videos.length} videos</span>
                {totalZaps > 0 && (
                  <span className="flex items-center gap-1 text-zap">
                    <Zap className="w-3.5 h-3.5" fill="currentColor" />
                    {totalZaps.toLocaleString()} sats
                  </span>
                )}
              </div>
            </div>
          </div>

          {profile?.about && (
            <p className="mt-4 text-sm text-secondary-foreground max-w-2xl whitespace-pre-wrap">{profile.about}</p>
          )}
        </div>
      </div>

      {/* Videos */}
      <div className="max-w-5xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-foreground">Videos</h3>
          <div className="flex items-center bg-secondary rounded-full p-0.5">
            <button
              onClick={() => setSortBy("recent")}
              className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium transition-all ${
                sortBy === "recent" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Clock className="w-3 h-3" />
              Recent
            </button>
            <button
              onClick={() => setSortBy("popular")}
              className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium transition-all ${
                sortBy === "popular" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <TrendingUp className="w-3 h-3" />
              Popular
            </button>
          </div>
        </div>

        {videosLoading && (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!videosLoading && longForm.length === 0 && (
          <div className="text-center py-20">
            <p className="text-muted-foreground">No videos published yet.</p>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 gap-y-8">
          {longForm.map((video) => (
            <VideoCard key={video.id} video={video} />
          ))}
        </div>
      </div>
    </div>
  );
};

export default Channel;
