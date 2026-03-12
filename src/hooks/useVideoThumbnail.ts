import { useState, useEffect } from "react";

// Known CORS-friendly video hosts
const CORS_FRIENDLY = ["nostr.build", "void.cat", "satellite.earth", "cdn.jb55.com"];

function isCorsLikely(url: string): boolean {
  try {
    const host = new URL(url).hostname;
    return CORS_FRIENDLY.some((h) => host.endsWith(h));
  } catch {
    return false;
  }
}

export function useVideoThumbnail(videoUrl: string, existingThumbnail?: string) {
  const [thumbnail, setThumbnail] = useState<string | null>(existingThumbnail || null);

  useEffect(() => {
    if (existingThumbnail) {
      setThumbnail(existingThumbnail);
      return;
    }

    // Skip thumbnail generation for non-CORS-friendly hosts to avoid console errors
    if (!videoUrl || !isCorsLikely(videoUrl)) return;

    let cancelled = false;
    const video = document.createElement("video");
    video.crossOrigin = "anonymous";
    video.muted = true;
    video.preload = "metadata";

    const cleanup = () => {
      video.removeAttribute("src");
      video.load();
    };

    video.addEventListener("loadeddata", () => {
      video.currentTime = Math.min(2, video.duration * 0.1);
    });

    video.addEventListener("seeked", () => {
      if (cancelled) return;
      try {
        const canvas = document.createElement("canvas");
        canvas.width = 480;
        canvas.height = 270;
        const ctx = canvas.getContext("2d");
        if (ctx) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const dataUrl = canvas.toDataURL("image/jpeg", 0.7);
          setThumbnail(dataUrl);
        }
      } catch {
        // CORS or other error — silently fail
      }
      cleanup();
    });

    video.addEventListener("error", () => {
      cleanup();
    });

    const timeout = setTimeout(() => {
      cancelled = true;
      cleanup();
    }, 8000);

    video.src = videoUrl;

    return () => {
      cancelled = true;
      clearTimeout(timeout);
      cleanup();
    };
  }, [videoUrl, existingThumbnail]);

  return thumbnail;
}
