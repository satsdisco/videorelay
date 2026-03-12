import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { ArrowLeft, Zap, Share2, ExternalLink } from "lucide-react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, timeAgo, type ParsedVideo } from "@/lib/nostr";
import { useNostrProfile } from "@/hooks/useNostrProfile";

import thumb1 from "@/assets/thumb-1.jpg";

const Watch = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [video, setVideo] = useState<ParsedVideo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;

    const fetchVideo = async () => {
      setLoading(true);
      try {
        const pool = getPool();
        const events = await pool.querySync(DEFAULT_RELAYS, {
          ids: [id],
          limit: 1,
        });

        if (events.length > 0) {
          const parsed = parseVideoEvent(events[0]);
          if (parsed) {
            setVideo(parsed);
          } else {
            setError("Could not parse video event");
          }
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
  const displayName = profile?.displayName || profile?.name || video?.pubkey?.slice(0, 12) + "...";

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-muted-foreground text-sm">Loading from relays...</p>
        </div>
      </div>
    );
  }

  if (error || !video) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <p className="text-muted-foreground mb-4">{error || "Video not found"}</p>
          <button
            onClick={() => navigate("/")}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
          >
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Top bar */}
      <div className="sticky top-0 z-50 flex items-center gap-3 px-4 py-3 bg-background/95 backdrop-blur-sm border-b border-border">
        <button
          onClick={() => navigate("/")}
          className="p-2 rounded-lg hover:bg-secondary transition-colors"
        >
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </button>
        <h1 className="text-sm font-medium text-foreground truncate">{video.title}</h1>
      </div>

      <div className="max-w-5xl mx-auto px-4 py-6">
        {/* Video Player */}
        <div className="relative aspect-video bg-secondary rounded-xl overflow-hidden mb-6">
          <video
            src={video.videoUrl}
            controls
            autoPlay
            className="w-full h-full"
            poster={video.thumbnail || thumb1}
          >
            Your browser does not support the video tag.
          </video>
        </div>

        {/* Video Info */}
        <div className="mb-6">
          <h2 className="text-xl font-bold text-foreground mb-2">{video.title}</h2>
          <div className="flex items-center justify-between flex-wrap gap-4">
            <div className="flex items-center gap-3">
              {profile?.picture ? (
                <img
                  src={profile.picture}
                  alt={displayName}
                  className="w-10 h-10 rounded-full object-cover"
                />
              ) : (
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary/80 to-zap flex items-center justify-center text-sm font-bold text-primary-foreground">
                  {displayName[0]?.toUpperCase() || "?"}
                </div>
              )}
              <div>
                <p className="text-sm font-semibold text-foreground">{displayName}</p>
                <p className="text-xs text-muted-foreground">{timeAgo(video.publishedAt)}</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button className="flex items-center gap-2 px-4 py-2 bg-primary/10 border border-primary/30 rounded-full hover:bg-primary/20 transition-colors">
                <Zap className="w-4 h-4 text-primary" fill="currentColor" />
                <span className="text-sm font-semibold text-primary">Zap</span>
              </button>
              <button className="flex items-center gap-2 px-4 py-2 bg-secondary rounded-full hover:bg-muted transition-colors">
                <Share2 className="w-4 h-4 text-foreground" />
                <span className="text-sm text-foreground">Share</span>
              </button>
              <a
                href={video.videoUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 px-4 py-2 bg-secondary rounded-full hover:bg-muted transition-colors"
              >
                <ExternalLink className="w-4 h-4 text-foreground" />
                <span className="text-sm text-foreground">Source</span>
              </a>
            </div>
          </div>
        </div>

        {/* Description */}
        {video.summary && (
          <div className="bg-secondary rounded-xl p-4 mb-6">
            <p className="text-sm text-secondary-foreground whitespace-pre-wrap">{video.summary}</p>
          </div>
        )}

        {/* Tags */}
        {video.tags.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-6">
            {video.tags.map((tag) => (
              <span
                key={tag}
                className="px-3 py-1 bg-primary/10 text-primary text-xs rounded-full font-medium"
              >
                #{tag}
              </span>
            ))}
          </div>
        )}

        {/* Event ID */}
        <div className="border-t border-border pt-4">
          <p className="text-xs text-muted-foreground">
            Event ID: <span className="font-mono">{video.id.slice(0, 24)}...</span>
          </p>
          <p className="text-xs text-muted-foreground">
            Pubkey: <span className="font-mono">{video.pubkey.slice(0, 24)}...</span>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Watch;
