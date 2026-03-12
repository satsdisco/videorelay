import { Zap, Eye, Clock } from "lucide-react";

export interface VideoData {
  id: string;
  thumbnail: string;
  title: string;
  creator: string;
  creatorNpub: string;
  views: string;
  zaps: string;
  duration: string;
  timeAgo: string;
  live?: boolean;
}

interface VideoCardProps {
  video: VideoData;
}

const VideoCard = ({ video }: VideoCardProps) => {
  return (
    <div className="group cursor-pointer">
      {/* Thumbnail */}
      <div className="relative aspect-video rounded-xl overflow-hidden bg-secondary mb-3">
        <img
          src={video.thumbnail}
          alt={video.title}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
        />
        {/* Duration badge */}
        <div className="absolute bottom-2 right-2 px-1.5 py-0.5 bg-background/90 rounded text-xs font-medium text-foreground backdrop-blur-sm">
          {video.duration}
        </div>
        {/* Live badge */}
        {video.live && (
          <div className="absolute top-2 left-2 flex items-center gap-1 px-2 py-0.5 bg-red-600 rounded text-xs font-bold text-primary-foreground">
            <span className="w-1.5 h-1.5 bg-primary-foreground rounded-full animate-pulse" />
            LIVE
          </div>
        )}
        {/* Hover overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-background/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
      </div>

      {/* Info */}
      <div className="flex gap-3">
        {/* Avatar */}
        <div className="w-9 h-9 shrink-0 rounded-full bg-gradient-to-br from-primary/80 to-zap flex items-center justify-center text-xs font-bold text-primary-foreground mt-0.5">
          {video.creator[0]}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-semibold text-foreground line-clamp-2 leading-snug group-hover:text-primary transition-colors">
            {video.title}
          </h3>
          <p className="text-xs text-muted-foreground mt-1 truncate">
            {video.creator}
          </p>
          <div className="flex items-center gap-3 mt-1">
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              <Eye className="w-3 h-3" />
              {video.views}
            </span>
            <span className="flex items-center gap-1 text-xs text-zap font-medium">
              <Zap className="w-3 h-3" fill="currentColor" />
              {video.zaps} sats
            </span>
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="w-3 h-3" />
              {video.timeAgo}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoCard;
