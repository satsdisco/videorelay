import { useState, useEffect } from "react";
import { getPool, DEFAULT_RELAYS, parseVideoEvent, timeAgo, type ParsedVideo, VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND } from "@/lib/nostr";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useVideoThumbnail } from "@/hooks/useVideoThumbnail";
import { useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";

import thumb1 from "@/assets/thumb-1.jpg";
import thumb2 from "@/assets/thumb-2.jpg";
import thumb3 from "@/assets/thumb-3.jpg";
import thumb4 from "@/assets/thumb-4.jpg";

const fallbackThumbs = [thumb1, thumb2, thumb3, thumb4];

const RelatedVideoItem = ({ video }: { video: ParsedVideo }) => {
  const { profile } = useNostrProfile(video.pubkey);
  const navigate = useNavigate();
  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 10) + "...";
  const generatedThumb = useVideoThumbnail(video.videoUrl, video.thumbnail || undefined);
  const thumbnail = generatedThumb || fallbackThumbs[video.id.charCodeAt(0) % fallbackThumbs.length];

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
          onError={(e) => {
            (e.target as HTMLImageElement).src = fallbackThumbs[0];
          }}
        />
        {video.duration && (
          <div className="absolute bottom-1 right-1 px-1 py-0.5 bg-background/90 rounded text-[10px] font-medium text-foreground">
            {video.duration}
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
  const [videos, setVideos] = useState<ParsedVideo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetch = async () => {
      setLoading(true);
      try {
        const pool = getPool();

        // Fetch by shared tags, or just recent if no tags
        const filter = tags.length > 0
          ? { kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND], "#t": [tags[0]], limit: 20 }
          : { kinds: [VIDEO_KIND, SHORT_VIDEO_KIND, ADDRESSABLE_VIDEO_KIND, ADDRESSABLE_SHORT_KIND], limit: 20 };

        const events = await pool.querySync(DEFAULT_RELAYS, filter);
        const parsed = events
          .map(parseVideoEvent)
          .filter((v): v is ParsedVideo => v !== null && v.id !== currentVideoId)
          .sort((a, b) => b.publishedAt - a.publishedAt)
          .slice(0, 12);

        setVideos(parsed);
      } catch (err) {
        console.error("Failed to fetch related videos:", err);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [currentVideoId, tags]);

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
