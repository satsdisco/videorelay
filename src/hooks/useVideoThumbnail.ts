import { useState, useEffect } from "react";

export function useVideoThumbnail(videoUrl: string, existingThumbnail?: string) {
  const [thumbnail, setThumbnail] = useState<string | null>(existingThumbnail || null);

  useEffect(() => {
    // If we already have a thumbnail, don't generate one
    if (existingThumbnail) {
      setThumbnail(existingThumbnail);
      return;
    }

    if (!videoUrl) return;

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
      // Seek to 2 seconds or 10% of duration
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

    // Timeout after 8s
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
