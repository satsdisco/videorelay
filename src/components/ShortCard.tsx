import { Zap, Play } from "lucide-react";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useVideoThumbnail } from "@/hooks/useVideoThumbnail";
import { timeAgo } from "@/lib/nostr";
import type { ParsedVideo } from "@/lib/nostr";
import { useNavigate } from "react-router-dom";

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

interface ShortCardProps {
  video: ParsedVideo;
}

const ShortCard = ({ video }: ShortCardProps) => {
  const { profile } = useNostrProfile(video.pubkey);
  const navigate = useNavigate();
  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 12) + "...";
  const generatedThumb = useVideoThumbnail(video.videoUrl, video.thumbnail || undefined);
  const thumbnail = generatedThumb || getFallbackThumb(video.id);

  return (
    <div
      className="group cursor-pointer shrink-0 w-[180px]"
      onClick={() => navigate(`/watch/${video.id}`)}
    >
      {/* Vertical thumbnail — 9:16 aspect */}
      <div className="relative aspect-[9/16] rounded-xl overflow-hidden bg-secondary mb-2">
        <img
          src={thumbnail}
          alt={video.title}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
          onError={(e) => {
            (e.target as HTMLImageElement).src = getFallbackThumb(video.id);
          }}
        />
        {/* Play overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-background/80 via-transparent to-transparent flex items-end justify-between p-3 opacity-0 group-hover:opacity-100 transition-opacity">
          <Play className="w-8 h-8 text-primary-foreground drop-shadow-lg" fill="currentColor" />
        </div>
        {/* Duration badge */}
        {video.duration && (
          <div className="absolute top-2 right-2 px-1.5 py-0.5 bg-background/80 rounded text-[10px] font-medium text-foreground backdrop-blur-sm">
            {video.duration}
          </div>
        )}
        {/* Shorts badge */}
        <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-primary/90 rounded text-[10px] font-bold text-primary-foreground backdrop-blur-sm">
          SHORT
        </div>
      </div>

      {/* Info */}
      <h3 className="text-xs font-semibold text-foreground line-clamp-2 leading-snug group-hover:text-primary transition-colors">
        {video.title}
      </h3>
      <p className="text-[11px] text-muted-foreground mt-0.5 truncate">{displayName}</p>
      <div className="flex items-center gap-2 mt-0.5">
        {video.zapCount > 0 && (
          <span className="flex items-center gap-0.5 text-[10px] text-accent font-medium">
            <Zap className="w-2.5 h-2.5" fill="currentColor" />
            {video.zapCount.toLocaleString()}
          </span>
        )}
        <span className="text-[10px] text-muted-foreground">{timeAgo(video.publishedAt)}</span>
      </div>
    </div>
  );
};

export default ShortCard;
