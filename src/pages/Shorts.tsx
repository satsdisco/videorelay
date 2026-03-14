import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Zap, Share2, MessageSquare, User } from "lucide-react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, timeAgo, type ParsedVideo, ALL_VIDEO_KINDS } from "@/lib/nostr";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { probeVideo, getCachedMeta, type VideoMeta } from "@/lib/durationProbe";
import type { Event } from "nostr-tools";

const ShortVideoItem = ({
  video,
  isActive,
}: {
  video: ParsedVideo;
  isActive: boolean;
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const navigate = useNavigate();
  const { profile } = useNostrProfile(video.pubkey);
  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 10) + "...";

  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    if (isActive) {
      v.play().catch(() => {});
    } else {
      v.pause();
      v.currentTime = 0;
    }
  }, [isActive]);

  const handleShare = async () => {
    const url = `${window.location.origin}/watch/${video.id}`;
    if (navigator.share) {
      try { await navigator.share({ title: video.title, url }); return; } catch {}
    }
    navigator.clipboard.writeText(url);
  };

  return (
    <div className="relative h-full w-full snap-start snap-always flex items-center justify-center bg-black">
      <video
        ref={videoRef}
        src={video.videoUrl}
        loop
        playsInline
        muted={false}
        className="h-full w-full object-contain"
        poster={video.thumbnail || undefined}
        onClick={(e) => {
          const v = e.currentTarget;
          v.paused ? v.play() : v.pause();
        }}
      />

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

        <button className="flex flex-col items-center gap-1">
          <div className="w-10 h-10 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
            <Zap className="w-5 h-5 text-primary" fill="currentColor" />
          </div>
          <span className="text-[10px] text-white font-medium">
            {video.zapCount > 0 ? video.zapCount.toLocaleString() : "Zap"}
          </span>
        </button>

        <button className="flex flex-col items-center gap-1">
          <div className="w-10 h-10 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
            <MessageSquare className="w-5 h-5 text-white" />
          </div>
          <span className="text-[10px] text-white font-medium">Chat</span>
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

  useEffect(() => {
    const fetchShorts = async () => {
      try {
        const pool = getPool();

        // Fetch lots of videos, then filter by aspect ratio
        const queries = [
          pool.querySync(DEFAULT_RELAYS.slice(0, 5), {
            kinds: ALL_VIDEO_KINDS as number[],
            limit: 300,
          }),
          // Also try tagged shorts
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

        // Dedupe
        const seen = new Set<string>();
        const deduped = parsed.filter((v) => {
          if (seen.has(v.id)) return false;
          seen.add(v.id);
          return true;
        });

        // Probe all candidates for duration + aspect ratio
        const probeResults = await Promise.allSettled(
          deduped.map(v => probeVideo(v.id, v.videoUrl))
        );

        // Filter: vertical videos OR short-tagged content
        const shorts = deduped.filter((v, i) => {
          const meta: VideoMeta | null = probeResults[i].status === "fulfilled"
            ? probeResults[i].value
            : getCachedMeta(v.id);

          // Vertical video = short (any duration)
          if (meta?.isVertical) return true;
          // Explicitly tagged as short
          if (v.isShort) return true;
          // Short duration (≤60s) even if horizontal
          const dur = meta?.duration || v.durationSeconds;
          return dur > 0 && dur <= 60;
        });

        // Shuffle — different order every visit
        for (let i = shorts.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [shorts[i], shorts[j]] = [shorts[j], shorts[i]];
        }

        setShorts(shorts);
      } catch (err) {
        console.error("Failed to fetch shorts:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchShorts();
  }, []);

  // Observe which short is in view
  const observerCallback = useCallback((entries: IntersectionObserverEntry[]) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        const idx = Number(entry.target.getAttribute("data-index"));
        if (!isNaN(idx)) setActiveIndex(idx);
      }
    }
  }, []);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const observer = new IntersectionObserver(observerCallback, {
      root: container,
      threshold: 0.6,
    });

    const items = container.querySelectorAll("[data-index]");
    items.forEach((el) => observer.observe(el));

    return () => observer.disconnect();
  }, [shorts, observerCallback]);

  if (loading) {
    return (
      <div className="h-screen bg-black flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
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
      {/* Back button */}
      <button
        onClick={() => navigate("/")}
        className="fixed top-4 left-4 z-50 p-2 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-colors"
      >
        <ArrowLeft className="w-5 h-5 text-white" />
      </button>

      {/* Shorts title */}
      <div className="fixed top-4 left-14 z-50">
        <span className="text-white font-bold text-lg">Shorts</span>
      </div>

      {/* Vertical snap scroll container */}
      <div
        ref={containerRef}
        className="h-full overflow-y-scroll snap-y snap-mandatory scrollbar-hide"
      >
        {shorts.map((video, idx) => (
          <div
            key={video.id}
            data-index={idx}
            className="h-screen w-full"
          >
            <ShortVideoItem video={video} isActive={idx === activeIndex} />
          </div>
        ))}
      </div>
    </div>
  );
};

export default Shorts;
