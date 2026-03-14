import { Zap, Clock, Eye } from "lucide-react";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { timeAgo, formatDate } from "@/lib/nostr";
import type { ParsedVideo } from "@/lib/nostr";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";

import { getFallbackThumb } from "@/lib/fallbackThumb";
import { hasWatched } from "@/lib/viewTracker";
import { getCachedPoster, extractPoster } from "@/lib/posterCache";
import { getCachedDuration, probeDuration, formatDurationSecs } from "@/lib/durationProbe";
import { isNsfw, getNsfwBlurEnabled } from "@/lib/nsfw";

interface VideoCardProps {
  video: ParsedVideo;
  /** Pre-fetched profile to avoid individual requests */
  cachedProfile?: { displayName?: string; name?: string; picture?: string } | null;
}

const VideoCard = ({ video, cachedProfile }: VideoCardProps) => {
  const { profile: fetchedProfile } = useNostrProfile(cachedProfile !== undefined ? null : video.pubkey);
  const profile = cachedProfile || fetchedProfile;
  const navigate = useNavigate();
  const [imgLoaded, setImgLoaded] = useState(false);

  const displayName = profile?.displayName || profile?.name || video.pubkey.slice(0, 12) + "...";
  const avatar = profile?.picture;
  const watched = hasWatched(video.id);

  // Try poster extraction for videos without thumbnails
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

  // Probe real duration if we don't have one
  const [probedDuration, setProbedDuration] = useState<number | null>(() => getCachedDuration(video.id));
  const hasNoDuration = !video.duration || video.duration === "0:00";

  useEffect(() => {
    if (hasNoDuration && probedDuration === null) {
      probeDuration(video.id, video.videoUrl).then(dur => {
        if (dur) setProbedDuration(dur);
      });
    }
  }, [video.id, video.videoUrl, hasNoDuration, probedDuration]);

  const displayDuration = hasNoDuration
    ? (probedDuration ? formatDurationSecs(probedDuration) : "")
    : video.duration;

  // NSFW detection
  const nsfwDetected = isNsfw({
    title: video.title,
    summary: video.summary,
    tags: video.tags,
    rawTags: video.rawEvent?.tags,
  });
  const blurEnabled = getNsfwBlurEnabled();
  const shouldBlur = nsfwDetected && blurEnabled;
  const [nsfwRevealed, setNsfwRevealed] = useState(false);

  return (
    <div
      className="group cursor-pointer"
      onClick={() => navigate(`/watch/${video.id}`)}
    >
      {/* Thumbnail */}
      <div className="relative aspect-video rounded-xl overflow-hidden bg-secondary mb-3">
        {!imgLoaded && <div className="absolute inset-0 bg-secondary animate-pulse" />}
        <img
          src={thumbnail}
          alt={video.title}
          className={`w-full h-full object-cover transition-transform duration-300 group-hover:scale-105 ${imgLoaded ? "opacity-100" : "opacity-0"}`}
          loading="lazy"
          decoding="async"
          onLoad={() => setImgLoaded(true)}
          onError={(e) => {
            (e.target as HTMLImageElement).src = getFallbackThumb(video.id);
            setImgLoaded(true);
          }}
        />
        {/* Duration badge */}
        {displayDuration && (
          <div className="absolute bottom-2 right-2 px-1.5 py-0.5 bg-background/90 rounded text-xs font-medium text-foreground backdrop-blur-sm">
            {displayDuration}
          </div>
        )}
        {/* Watched badge */}
        {watched && (
          <div className="absolute top-2 left-2 flex items-center gap-1 px-1.5 py-0.5 bg-background/80 rounded text-[10px] font-medium text-muted-foreground backdrop-blur-sm">
            <Eye className="w-3 h-3" />
            Watched
          </div>
        )}
        {/* NSFW blur overlay */}
        {shouldBlur && !nsfwRevealed && (
          <div
            className="absolute inset-0 backdrop-blur-xl bg-background/40 flex flex-col items-center justify-center z-10"
            onClick={(e) => { e.stopPropagation(); setNsfwRevealed(true); }}
          >
            <span className="text-lg mb-1">🔞</span>
            <span className="text-xs font-medium text-foreground/80">Sensitive content</span>
            <span className="text-[10px] text-muted-foreground mt-0.5">Click to reveal</span>
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
            loading="lazy"
            decoding="async"
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = "none";
            }}
          />
        ) : (
          <div className="w-9 h-9 shrink-0 rounded-full bg-gradient-to-br from-primary/80 to-accent flex items-center justify-center text-xs font-bold text-primary-foreground mt-0.5">
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
            {displayName}
          </p>
          <div className="flex items-center gap-3 mt-1">
            {video.zapCount > 0 && (
              <span className="flex items-center gap-1 text-xs text-primary font-medium">
                <Zap className="w-3 h-3" fill="currentColor" />
                {video.zapCount.toLocaleString()} sats
              </span>
            )}
            <span className="flex items-center gap-1 text-xs text-muted-foreground" title={formatDate(video.publishedAt)}>
              <Clock className="w-3 h-3" />
              {formatDate(video.publishedAt)} · {timeAgo(video.publishedAt)}
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
