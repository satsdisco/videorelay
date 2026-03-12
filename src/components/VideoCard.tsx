import { Zap, Eye, Clock } from "lucide-react";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useVideoThumbnail } from "@/hooks/useVideoThumbnail";
import { timeAgo } from "@/lib/nostr";
import type { ParsedVideo } from "@/lib/nostr";
import { useNavigate } from "react-router-dom";

// Fallback thumbnails for videos without thumbnails
import thumb1 from "@/assets/thumb-1.jpg";
import thumb2 from "@/assets/thumb-2.jpg";
import thumb3 from "@/assets/thumb-3.jpg";
import thumb4 from "@/assets/thumb-4.jpg";
import thumb5 from "@/assets/thumb-5.jpg";
import thumb6 from "@/assets/thumb-6.jpg";
import thumb7 from "@/assets/thumb-7.jpg";
import thumb8 from "@/assets/thumb-8.jpg";

const fallbackThumbs = [thumb1, thumb2, thumb3, thumb4, thumb5, thumb6, thumb7, thumb8];

function getFallbackThumb(id: string): string {
  const index = id.charCodeAt(0) % fallbackThumbs.length;
  return fallbackThumbs[index];
}

interface VideoCardProps {
  video: ParsedVideo;
}

const VideoCard = ({ video }: VideoCardProps) => {
  const { profile } = useNostrProfile(video.pubkey);
  const navigate = useNavigate();
  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 12) + "...";
  const avatar = profile?.picture;
  const generatedThumb = useVideoThumbnail(video.videoUrl, video.thumbnail || undefined);
  const thumbnail = generatedThumb || getFallbackThumb(video.id);

  return (
    <div
      className="group cursor-pointer"
      onClick={() => navigate(`/watch/${video.id}`)}
    >
      {/* Thumbnail */}
      <div className="relative aspect-video rounded-xl overflow-hidden bg-secondary mb-3">
        <img
          src={thumbnail}
          alt={video.title}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
          onError={(e) => {
            (e.target as HTMLImageElement).src = getFallbackThumb(video.id);
          }}
        />
        {/* Duration badge */}
        {video.duration && (
          <div className="absolute bottom-2 right-2 px-1.5 py-0.5 bg-background/90 rounded text-xs font-medium text-foreground backdrop-blur-sm">
            {video.duration}
          </div>
        )}
        {/* Hover overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-background/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
      </div>

      {/* Info */}
      <div className="flex gap-3">
        {/* Avatar */}
        {avatar ? (
          <img
            src={avatar}
            alt={displayName}
            className="w-9 h-9 shrink-0 rounded-full object-cover mt-0.5"
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = "none";
            }}
          />
        ) : (
          <div className="w-9 h-9 shrink-0 rounded-full bg-gradient-to-br from-primary/80 to-zap flex items-center justify-center text-xs font-bold text-primary-foreground mt-0.5">
            {displayName[0]?.toUpperCase() || "?"}
          </div>
        )}
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-semibold text-foreground line-clamp-2 leading-snug group-hover:text-primary transition-colors">
            {video.title}
          </h3>
          <p
            className="text-xs text-muted-foreground mt-1 truncate cursor-pointer hover:text-primary transition-colors"
            onClick={(e) => {
              e.stopPropagation();
              navigate(`/channel/${video.pubkey}`);
            }}
          >
          <div className="flex items-center gap-3 mt-1">
            {video.zapCount > 0 && (
              <span className="flex items-center gap-1 text-xs text-zap font-medium">
                <Zap className="w-3 h-3" fill="currentColor" />
                {video.zapCount.toLocaleString()} sats
              </span>
            )}
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="w-3 h-3" />
              {timeAgo(video.publishedAt)}
            </span>
            {video.tags.length > 0 && (
              <span className="text-xs text-primary/70 truncate">
                #{video.tags[0]}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoCard;
