import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Zap, Share2, MessageSquare, Volume2, VolumeX, Play } from "lucide-react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, timeAgo, type ParsedVideo, ALL_VIDEO_KINDS } from "@/lib/nostr";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { probeVideo, getCachedMeta, type VideoMeta } from "@/lib/durationProbe";
import { getCachedPoster } from "@/lib/posterCache";
import { useToast } from "@/hooks/use-toast";
import { isNsfw, getNsfwBlurEnabled } from "@/lib/nsfw";
import type { Event } from "nostr-tools";

// Global mute state shared across all shorts
let globalMuted = true;

const ShortVideoItem = ({
  video,
  isActive,
  onMuteToggle,
  muted,
}: {
  video: ParsedVideo;
  isActive: boolean;
  onMuteToggle: () => void;
  muted: boolean;
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const navigate = useNavigate();
  const { profile } = useNostrProfile(video.pubkey);
  const { toast } = useToast();
  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 10) + "...";
  const [paused, setPaused] = useState(false);
  const poster = video.thumbnail || getCachedPoster(video.id) || undefined;

  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    v.muted = muted;
  }, [muted]);

  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    if (isActive) {
      v.muted = globalMuted;
      v.currentTime = 0;
      const playPromise = v.play();
      if (playPromise) {
        playPromise.catch(() => {
          // Autoplay blocked — try muted
          v.muted = true;
          v.play().catch(() => {});
        });
      }
      setPaused(false);
    } else {
      v.pause();
      v.currentTime = 0;
      setPaused(false);
    }
  }, [isActive]);

  const togglePlay = () => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) {
      v.play().catch(() => {});
      setPaused(false);
    } else {
      v.pause();
      setPaused(true);
    }
  };

  const handleShare = async () => {
    const url = `${window.location.origin}/watch/${video.id}`;
    if (navigator.share) {
      try { await navigator.share({ title: video.title, url }); return; } catch {}
    }
    await navigator.clipboard.writeText(url);
    toast({ title: "Link copied!" });
  };

  const handleZap = () => {
    // Navigate to watch page where full zap flow lives
    navigate(`/watch/${video.id}`);
  };

  const handleComment = () => {
    navigate(`/watch/${video.id}`);
  };

  return (
    <div className="relative h-full w-full snap-start snap-always flex items-center justify-center bg-black">
      <video
        ref={videoRef}
        src={video.videoUrl}
        loop
        playsInline
        muted={muted}
        className="h-full w-full object-contain"
        poster={poster}
        preload="auto"
        onClick={togglePlay}
      />

      {/* Play indicator when paused */}
      {paused && (
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className="w-16 h-16 bg-black/40 rounded-full flex items-center justify-center backdrop-blur-sm">
            <Play className="w-8 h-8 text-white ml-1" fill="white" />
          </div>
        </div>
      )}

      {/* Mute toggle */}
      <button
        onClick={(e) => { e.stopPropagation(); onMuteToggle(); }}
        className="absolute top-4 right-4 z-10 p-2.5 rounded-full bg-black/40 backdrop-blur-sm"
      >
        {muted ? <VolumeX className="w-5 h-5 text-white" /> : <Volume2 className="w-5 h-5 text-white" />}
      </button>

      {/* Right action buttons */}
      <div className="absolute right-3 bottom-28 flex flex-col items-center gap-5 z-10">
        <button
          onClick={() => navigate(`/channel/${video.pubkey}`)}
          className="flex flex-col items-center gap-1"
        >
          {profile?.picture ? (
            <img src={profile.picture} alt="" className="w-10 h-10 rounded-full object-cover border-2 border-primary" />
          ) : (
            <div className="w-10 h-10 rounded-full bg-primary/80 flex items-center justify-center text-xs font-bold text-primary-foreground">
              {displayName[0]?.toUpperCase()}
            </div>
          )}
        </button>

        <button onClick={handleZap} className="flex flex-col items-center gap-1">
          <div className="w-10 h-10 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
            <Zap className="w-5 h-5 text-primary" fill="currentColor" />
          </div>
          <span className="text-[10px] text-white font-medium">
            {video.zapCount > 0 ? video.zapCount.toLocaleString() : "Zap"}
          </span>
        </button>

        <button onClick={handleComment} className="flex flex-col items-center gap-1">
          <div className="w-10 h-10 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
            <MessageSquare className="w-5 h-5 text-white" />
          </div>
          <span className="text-[10px] text-white font-medium">Comments</span>
        </button>

        <button onClick={handleShare} className="flex flex-col items-center gap-1">
          <div className="w-10 h-10 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
            <Share2 className="w-5 h-5 text-white" />
          </div>
          <span className="text-[10px] text-white font-medium">Share</span>
        </button>
      </div>

      {/* Bottom info */}
      <div className="absolute left-3 right-16 bottom-8 z-10">
        <p
          className="text-sm font-bold text-white cursor-pointer hover:underline"
          onClick={() => navigate(`/channel/${video.pubkey}`)}
        >
          @{displayName}
        </p>
        <p className="text-xs text-white/80 mt-1 line-clamp-2">{video.title}</p>
        {video.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1.5">
            {video.tags.slice(0, 3).map((t) => (
              <span key={t} className="text-[10px] text-primary font-medium">#{t}</span>
            ))}
          </div>
        )}
      </div>

      {/* Gradient overlays */}
      <div className="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-black/70 to-transparent pointer-events-none" />
      <div className="absolute inset-x-0 top-0 h-20 bg-gradient-to-b from-black/50 to-transparent pointer-events-none" />
    </div>
  );
};

const Shorts = () => {
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);
  const [shorts, setShorts] = useState<ParsedVideo[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeIndex, setActiveIndex] = useState(0);
  const [muted, setMuted] = useState(true);

  const toggleMute = useCallback(() => {
    setMuted(prev => {
      globalMuted = !prev;
      return !prev;
    });
  }, []);

  useEffect(() => {
    const fetchShorts = async () => {
      try {
        const pool = getPool();

        const queries = [
          pool.querySync(DEFAULT_RELAYS.slice(0, 5), {
            kinds: ALL_VIDEO_KINDS as number[],
            limit: 300,
          }),
          pool.querySync(DEFAULT_RELAYS.slice(0, 4), {
            kinds: ALL_VIDEO_KINDS as number[],
            "#t": ["shorts", "short", "clip", "reel", "vertical"],
            limit: 100,
          }),
        ];

        const results = await Promise.allSettled(queries);
        const allEvents: Event[] = [];
        for (const r of results) {
          if (r.status === "fulfilled") allEvents.push(...r.value);
        }

        const parsed = allEvents
          .map(parseVideoEvent)
          .filter((v): v is ParsedVideo => v !== null);

        const seen = new Set<string>();
        const deduped = parsed.filter((v) => {
          if (seen.has(v.id)) return false;
          seen.add(v.id);
          return true;
        });

        const probeResults = await Promise.allSettled(
          deduped.map(v => probeVideo(v.id, v.videoUrl))
        );

        const filtered = deduped.filter((v, i) => {
          const meta: VideoMeta | null = probeResults[i].status === "fulfilled"
            ? probeResults[i].value
            : getCachedMeta(v.id);

          if (meta?.isVertical) return true;
          if (v.isShort) return true;
          const dur = meta?.duration || v.durationSeconds;
          return dur > 0 && dur <= 60;
        });

        // Filter out NSFW from autoplay feed
        if (getNsfwBlurEnabled()) {
          filtered = filtered.filter(v => !isNsfw({
            title: v.title, summary: v.summary, tags: v.tags, rawTags: v.rawEvent?.tags,
          }));
        }

        // Shuffle
        for (let i = filtered.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [filtered[i], filtered[j]] = [filtered[j], filtered[i]];
        }

        setShorts(filtered);
      } catch (err) {
        console.error("Failed to fetch shorts:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchShorts();
  }, []);

  // Intersection observer for active detection
  useEffect(() => {
    const container = containerRef.current;
    if (!container || shorts.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const idx = Number(entry.target.getAttribute("data-index"));
            if (!isNaN(idx)) setActiveIndex(idx);
          }
        }
      },
      { root: container, threshold: 0.6 }
    );

    // Small delay to ensure DOM is ready
    const timer = setTimeout(() => {
      const items = container.querySelectorAll("[data-index]");
      items.forEach((el) => observer.observe(el));
    }, 100);

    return () => {
      clearTimeout(timer);
      observer.disconnect();
    };
  }, [shorts]);

  if (loading) {
    return (
      <div className="h-screen bg-black flex flex-col items-center justify-center gap-3">
        <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
        <p className="text-white/60 text-sm">Finding shorts...</p>
      </div>
    );
  }

  if (shorts.length === 0) {
    return (
      <div className="h-screen bg-black flex flex-col items-center justify-center text-white px-4">
        <p className="text-lg font-bold mb-2">No Shorts found</p>
        <p className="text-sm text-white/60 mb-4">Check back later for short-form content</p>
        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-lg"
        >
          Go Home
        </button>
      </div>
    );
  }

  return (
    <div className="h-screen bg-black relative">
      <button
        onClick={() => navigate("/")}
        className="fixed top-4 left-4 z-50 p-2 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-colors"
      >
        <ArrowLeft className="w-5 h-5 text-white" />
      </button>

      <div className="fixed top-4 left-14 z-50">
        <span className="text-white font-bold text-lg">Shorts</span>
        <span className="text-white/40 text-sm ml-2">{shorts.length} videos</span>
      </div>

      <div
        ref={containerRef}
        className="h-full overflow-y-scroll snap-y snap-mandatory scrollbar-hide"
      >
        {shorts.map((video, idx) => (
          <div key={video.id} data-index={idx} className="h-screen w-full">
            <ShortVideoItem
              video={video}
              isActive={idx === activeIndex}
              onMuteToggle={toggleMute}
              muted={muted}
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default Shorts;
