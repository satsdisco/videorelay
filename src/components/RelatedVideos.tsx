import { useState, useEffect } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, timeAgo, type ParsedVideo, ALL_VIDEO_KINDS } from "@/lib/nostr";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { getCachedVideos } from "@/lib/videoCache";
import { getFallbackThumb } from "@/lib/fallbackThumb";
import { getCachedPoster, extractPoster } from "@/lib/posterCache";
import { getCachedDuration, probeDuration, formatDurationSecs } from "@/lib/durationProbe";

const RelatedVideoItem = ({ video }: { video: ParsedVideo }) => {
  const { profile } = useNostrProfile(video.pubkey);
  const navigate = useNavigate();
  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 10) + "...";

  // Poster extraction for videos without thumbnails
  const [posterUrl, setPosterUrl] = useState<string | null>(() => getCachedPoster(video.id));
  const hasThumbnail = !!video.thumbnail;

  useEffect(() => {
    if (!hasThumbnail && !posterUrl) {
      extractPoster(video.id, video.videoUrl).then(url => {
        if (url) setPosterUrl(url);
      });
    }
  }, [video.id, video.videoUrl, hasThumbnail, posterUrl]);

  const thumbnail = video.thumbnail || posterUrl || getFallbackThumb(video.id);

  // Duration probe
  const [probedDur, setProbedDur] = useState<number | null>(() => getCachedDuration(video.id));
  const noDur = !video.duration || video.duration === "0:00";

  useEffect(() => {
    if (noDur && probedDur === null) {
      probeDuration(video.id, video.videoUrl).then(d => { if (d) setProbedDur(d); });
    }
  }, [video.id, video.videoUrl, noDur, probedDur]);

  const displayDuration = noDur ? (probedDur ? formatDurationSecs(probedDur) : "") : video.duration;

  return (
    <div
      className="flex gap-2 cursor-pointer group"
      onClick={() => navigate(`/watch/${video.id}`)}
    >
      <div className="relative w-40 shrink-0 aspect-video rounded-lg overflow-hidden bg-secondary">
        <img
          src={thumbnail}
          alt={video.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
          decoding="async"
          onError={(e) => {
            (e.target as HTMLImageElement).src = getFallbackThumb(video.id);
          }}
        />
        {displayDuration && (
          <div className="absolute bottom-1 right-1 px-1 py-0.5 bg-background/90 rounded text-[10px] font-medium text-foreground">
            {displayDuration}
          </div>
        )}
      </div>
      <div className="flex-1 min-w-0">
        <h4 className="text-sm font-semibold text-foreground line-clamp-2 leading-snug group-hover:text-primary transition-colors">
          {video.title}
        </h4>
        <p className="text-xs text-muted-foreground mt-1 truncate">{displayName}</p>
        <p className="text-xs text-muted-foreground">{timeAgo(video.publishedAt)}</p>
      </div>
    </div>
  );
};

interface RelatedVideosProps {
  currentVideoId: string;
  tags: string[];
}

const RelatedVideos = ({ currentVideoId, tags }: RelatedVideosProps) => {
  const [videos, setVideos] = useState<ParsedVideo[]>(() => {
    // Try cache first for instant related videos
    const cached = getCachedVideos();
    if (cached.length > 0) {
      const tagSet = new Set(tags.map(t => t.toLowerCase()));
      return cached
        .filter(v => v.id !== currentVideoId)
        .sort((a, b) => {
          const aMatch = a.tags.some(t => tagSet.has(t.toLowerCase()));
          const bMatch = b.tags.some(t => tagSet.has(t.toLowerCase()));
          if (aMatch && !bMatch) return -1;
          if (!aMatch && bMatch) return 1;
          return b.publishedAt - a.publishedAt;
        })
        .slice(0, 12);
    }
    return [];
  });
  const [loading, setLoading] = useState(videos.length === 0);

  // Fetch from relays in background to supplement cache
  useState(() => {
    if (videos.length >= 8) {
      setLoading(false);
      return;
    }
    const fetchRelated = async () => {
      try {
        const pool = getPool();
        const filter = tags.length > 0
          ? { kinds: ALL_VIDEO_KINDS as number[], "#t": [tags[0]], limit: 20 }
          : { kinds: ALL_VIDEO_KINDS as number[], limit: 20 };

        const events = await pool.querySync(DEFAULT_RELAYS, filter);
        const parsed = events
          .map(parseVideoEvent)
          .filter((v): v is ParsedVideo => v !== null && v.id !== currentVideoId)
          .sort((a, b) => b.publishedAt - a.publishedAt)
          .slice(0, 12);

        if (parsed.length > 0) setVideos(parsed);
      } catch (err) {
        console.error("Failed to fetch related videos:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchRelated();
  });

  return (
    <div>
      <h3 className="text-sm font-bold text-foreground mb-3">Related Videos</h3>
      {loading ? (
        <div className="flex items-center gap-2 py-4 text-muted-foreground">
          <Loader2 className="w-4 h-4 animate-spin" />
          <span className="text-xs">Finding related content...</span>
        </div>
      ) : videos.length === 0 ? (
        <p className="text-xs text-muted-foreground">No related videos found.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {videos.map((video) => (
            <RelatedVideoItem key={video.id} video={video} />
          ))}
        </div>
      )}
    </div>
  );
};

export default RelatedVideos;
