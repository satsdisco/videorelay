import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { ArrowLeft, Zap, Share2, ExternalLink, UserPlus, UserCheck, Loader2 } from "lucide-react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, timeAgo, type ParsedVideo } from "@/lib/nostr";
import { getCachedVideo, cacheVideos } from "@/lib/videoCache";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useNostrFollow } from "@/hooks/useNostrFollow";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import { getRandomLoadingMessage, getRandomErrorMessage } from "@/lib/loadingMessages";
import { useZap } from "@/hooks/useZap";
import { recordView } from "@/lib/viewTracker";
import VideoPlayer from "@/components/VideoPlayer";
import VideoComments from "@/components/VideoComments";
import RelatedVideos from "@/components/RelatedVideos";

import { getFallbackThumb } from "@/lib/fallbackThumb";

const Watch = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  // Try cache first for instant display
  const [video, setVideo] = useState<ParsedVideo | null>(() => id ? getCachedVideo(id) : null);
  const [loading, setLoading] = useState(!video);
  const [error, setError] = useState<string | null>(null);
  const [showZapModal, setShowZapModal] = useState(false);
  const [copied, setCopied] = useState(false);

  const { isLoggedIn, pubkey: myPubkey } = useNostrAuth();
  const { zap, loading: zapLoading, success: zapSuccess, error: zapError, invoice: zapInvoice } = useZap();

  // Record view
  useEffect(() => {
    if (id) recordView(id);
  }, [id]);

  useEffect(() => {
    if (!id) return;
    // If already cached, skip relay fetch
    const cached = getCachedVideo(id);
    if (cached) {
      setVideo(cached);
      setLoading(false);
      return;
    }
    const fetchVideo = async () => {
      setLoading(true);
      try {
        const pool = getPool();
        const events = await pool.querySync(DEFAULT_RELAYS, { ids: [id], limit: 1 });
        if (events.length > 0) {
          const parsed = parseVideoEvent(events[0]);
          if (parsed) {
            cacheVideos([parsed]);
            setVideo(parsed);
          } else setError("Could not parse video event");
        } else {
          setError("Video not found on relays");
        }
      } catch {
        setError("Failed to fetch video");
      } finally {
        setLoading(false);
      }
    };
    fetchVideo();
  }, [id]);

  const { profile } = useNostrProfile(video?.pubkey ?? null);
  const { isFollowing, loading: followLoading, toggleFollow } = useNostrFollow(video?.pubkey ?? null);
  const displayName = profile?.displayName || profile?.name || video?.pubkey?.slice(0, 12) + "...";
  const isOwnChannel = myPubkey && video?.pubkey === myPubkey;

  const handleShare = async () => {
    const url = window.location.href;
    if (navigator.share) {
      try { await navigator.share({ title: video?.title, url }); return; } catch {}
    }
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {}
  };

  const handleZap = async (amount: number) => {
    if (!video) return;
    await zap({ eventId: video.id, recipientPubkey: video.pubkey }, amount);
    // Keep modal open if we got an invoice (no WebLN)
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center px-4">
        <div className="flex flex-col items-center gap-3 max-w-md text-center">
          <div className="w-10 h-10 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-foreground font-medium">Fetching from the Nostr-verse...</p>
          <p className="text-muted-foreground text-sm">{getRandomLoadingMessage()}</p>
        </div>
      </div>
    );
  }

  if (error || !video) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center px-4">
        <div className="text-center max-w-md">
          <p className="text-foreground font-medium mb-2">Well, that didn't work.</p>
          <p className="text-muted-foreground text-sm mb-4">{getRandomErrorMessage()}</p>
          <button onClick={() => navigate("/")} className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity">
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-16 md:pb-0">
      {/* Top bar */}
      <div className="sticky top-0 z-50 flex items-center gap-3 px-3 md:px-4 py-3 bg-background/95 backdrop-blur-sm border-b border-border">
        <button onClick={() => navigate("/")} className="p-2 rounded-lg hover:bg-secondary transition-colors">
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </button>
        <h1 className="text-sm font-medium text-foreground truncate">{video.title}</h1>
      </div>

      <div className="flex flex-col lg:flex-row gap-4 md:gap-6 max-w-[1400px] mx-auto px-0 md:px-4 py-0 md:py-6">
        {/* Main content */}
        <div className="flex-1 min-w-0">
          {/* Custom Video Player */}
          <div className="mb-4 md:mb-6">
            <VideoPlayer
              src={video.videoUrl}
              poster={video.thumbnail || getFallbackThumb(video.id)}
            />
          </div>

          {/* Video Info */}
          <div className="px-3 md:px-0 mb-4 md:mb-6">
            <h2 className="text-base md:text-xl font-bold text-foreground mb-2">{video.title}</h2>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              {/* Creator + Follow */}
              <div className="flex items-center gap-3">
                <div
                  className="flex items-center gap-3 cursor-pointer"
                  onClick={() => navigate(`/channel/${video.pubkey}`)}
                >
                  {profile?.picture ? (
                    <img src={profile.picture} alt={displayName} className="w-10 h-10 rounded-full object-cover" />
                  ) : (
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary/80 to-accent flex items-center justify-center text-sm font-bold text-primary-foreground">
                      {displayName[0]?.toUpperCase() || "?"}
                    </div>
                  )}
                  <div>
                    <p className="text-sm font-semibold text-foreground">{displayName}</p>
                    <p className="text-xs text-muted-foreground">{timeAgo(video.publishedAt)}</p>
                  </div>
                </div>

                {/* Follow button */}
                {isLoggedIn && !isOwnChannel && (
                  <button
                    onClick={toggleFollow}
                    disabled={followLoading}
                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
                      isFollowing
                        ? "bg-secondary text-foreground hover:bg-destructive/20 hover:text-destructive"
                        : "bg-primary text-primary-foreground hover:opacity-90"
                    }`}
                  >
                    {followLoading ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : isFollowing ? (
                      <UserCheck className="w-3.5 h-3.5" />
                    ) : (
                      <UserPlus className="w-3.5 h-3.5" />
                    )}
                    {isFollowing ? "Following" : "Follow"}
                  </button>
                )}
              </div>

              {/* Action buttons */}
              <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide -mx-3 px-3 md:mx-0 md:px-0">
                <div className="relative shrink-0">
                  <button
                    onClick={() => !zapLoading && setShowZapModal(!showZapModal)}
                    disabled={zapLoading}
                    className={`flex items-center gap-2 px-4 py-2 border rounded-full transition-colors ${
                      zapSuccess
                        ? "bg-green-500/10 border-green-500/30 text-green-500"
                        : zapError
                        ? "bg-destructive/10 border-destructive/30 text-destructive"
                        : "bg-primary/10 border-primary/30 text-primary hover:bg-primary/20"
                    }`}
                  >
                    {zapLoading ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <Zap className="w-4 h-4" fill="currentColor" />
                    )}
                    <span className="text-sm font-semibold whitespace-nowrap">
                      {zapLoading ? "Zapping..." : zapSuccess ? "⚡ Zapped!" : zapError ? "Failed" : "Zap"}
                    </span>
                  </button>
                  {showZapModal && !zapInvoice && (
                    <div className="absolute top-full mt-2 right-0 md:right-0 left-0 md:left-auto bg-background border border-border rounded-xl shadow-xl p-3 z-50 w-56">
                      <p className="text-xs text-muted-foreground mb-2">Send sats ⚡</p>
                      <div className="grid grid-cols-3 gap-2">
                        {[21, 100, 500, 1000, 5000, 10000].map((amt) => (
                          <button
                            key={amt}
                            onClick={() => handleZap(amt)}
                            className="px-2 py-2 bg-secondary hover:bg-primary hover:text-primary-foreground rounded-lg text-xs font-medium text-foreground transition-colors"
                          >
                            {amt.toLocaleString()}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                  {zapInvoice && (
                    <div className="absolute top-full mt-2 left-0 bg-background border border-border rounded-xl shadow-xl p-4 z-50 w-72">
                      <p className="text-xs font-semibold text-foreground mb-2">⚡ Pay this invoice</p>
                      <div className="bg-secondary rounded-lg p-2 mb-3">
                        <p className="text-[10px] text-muted-foreground break-all font-mono leading-relaxed select-all">
                          {zapInvoice}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => {
                            navigator.clipboard.writeText(zapInvoice);
                          }}
                          className="flex-1 px-3 py-2 bg-primary text-primary-foreground rounded-lg text-xs font-medium hover:bg-primary/90 transition-colors"
                        >
                          Copy Invoice
                        </button>
                        <a
                          href={`lightning:${zapInvoice}`}
                          className="flex-1 px-3 py-2 bg-secondary text-foreground rounded-lg text-xs font-medium text-center hover:bg-muted transition-colors"
                        >
                          Open Wallet
                        </a>
                      </div>
                      <button
                        onClick={() => setShowZapModal(false)}
                        className="w-full mt-2 text-xs text-muted-foreground hover:text-foreground transition-colors"
                      >
                        Close
                      </button>
                    </div>
                  )}
                </div>
                <button onClick={handleShare} className="flex items-center gap-2 px-4 py-2 bg-secondary rounded-full hover:bg-muted transition-colors shrink-0">
                  <Share2 className="w-4 h-4 text-foreground" />
                  <span className="text-sm text-foreground">{copied ? "Copied!" : "Share"}</span>
                </button>
                <a href={video.videoUrl} target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 px-4 py-2 bg-secondary rounded-full hover:bg-muted transition-colors shrink-0">
                  <ExternalLink className="w-4 h-4 text-foreground" />
                  <span className="text-sm text-foreground">Source</span>
                </a>
              </div>
            </div>
          </div>

          {/* Description */}
          {video.summary && (
            <div className="mx-3 md:mx-0 bg-secondary rounded-xl p-4 mb-4 md:mb-6">
              <p className="text-sm text-secondary-foreground whitespace-pre-wrap">{video.summary}</p>
            </div>
          )}

          {/* Tags */}
          {video.tags.length > 0 && (
            <div className="flex flex-wrap gap-2 px-3 md:px-0 mb-4 md:mb-6">
              {video.tags.map((tag) => (
                <span
                  key={tag}
                  className="px-3 py-1 bg-primary/10 text-primary text-xs rounded-full font-medium cursor-pointer hover:bg-primary/20 transition-colors"
                  onClick={() => navigate(`/?tag=${tag}`)}
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}

          {/* Comments */}
          <div className="border-t border-border pt-4 md:pt-6 px-3 md:px-0">
            <VideoComments videoId={video.id} />
          </div>
        </div>

        {/* Sidebar */}
        <div className="w-full lg:w-[360px] shrink-0 px-3 md:px-0">
          <RelatedVideos currentVideoId={video.id} tags={video.tags} />
        </div>
      </div>
    </div>
  );
};

export default Watch;
