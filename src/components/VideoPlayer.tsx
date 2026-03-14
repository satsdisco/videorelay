import { useRef, useState, useEffect, useCallback } from "react";
import {
  Play, Pause, Volume2, VolumeX, Maximize, Minimize,
  PictureInPicture2, SkipBack, SkipForward, Settings,
} from "lucide-react";

function isHlsUrl(url: string): boolean {
  return /\.m3u8(\?|$)/i.test(url);
}

interface VideoPlayerProps {
  src: string;
  poster?: string;
  autoPlay?: boolean;
  onEnded?: () => void;
}

const VideoPlayer = ({ src, poster, autoPlay = true, onEnded }: VideoPlayerProps) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const progressRef = useRef<HTMLDivElement>(null);
  const hideTimeout = useRef<ReturnType<typeof setTimeout>>();

  const [playing, setPlaying] = useState(autoPlay);
  const [muted, setMuted] = useState(false);
  const [volume, setVolume] = useState(1);
  const [currentTime, setCurrent] = useState(0);
  const [duration, setDuration] = useState(0);
  const [buffered, setBuffered] = useState(0);
  const [fullscreen, setFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [pipSupported, setPipSupported] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [showSpeed, setShowSpeed] = useState(false);

  // HLS setup — lazy load hls.js only when needed
  useEffect(() => {
    const v = videoRef.current;
    if (!v || !isHlsUrl(src)) return;

    // Safari handles HLS natively
    if (v.canPlayType("application/vnd.apple.mpegurl")) {
      v.src = src;
      return;
    }

    let hls: any = null;
    let destroyed = false;

    import("hls.js").then(({ default: Hls }) => {
      if (destroyed || !Hls.isSupported()) return;

      hls = new Hls({
        enableWorker: true,
        startLevel: -1,
      });
      hls.loadSource(src);
      hls.attachMedia(v);
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        if (autoPlay) v.play().catch(() => {});
      });
      hls.on(Hls.Events.ERROR, (_event: any, data: any) => {
        if (data.fatal) {
          console.error("HLS fatal error:", data);
          if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
            hls.startLoad();
          } else {
            hls.destroy();
          }
        }
      });
    });

    return () => {
      destroyed = true;
      if (hls) hls.destroy();
    };
  }, [src, autoPlay]);

  // Check PiP support
  useEffect(() => {
    setPipSupported("pictureInPictureEnabled" in document);
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      const v = videoRef.current;
      if (!v) return;
      // Don't capture when typing in inputs
      if ((e.target as HTMLElement).tagName === "INPUT" || (e.target as HTMLElement).tagName === "TEXTAREA") return;

      switch (e.key.toLowerCase()) {
        case " ":
        case "k":
          e.preventDefault();
          v.paused ? v.play() : v.pause();
          break;
        case "f":
          e.preventDefault();
          toggleFullscreen();
          break;
        case "m":
          e.preventDefault();
          v.muted = !v.muted;
          setMuted(v.muted);
          break;
        case "arrowleft":
        case "j":
          e.preventDefault();
          v.currentTime = Math.max(0, v.currentTime - (e.key === "j" ? 10 : 5));
          break;
        case "arrowright":
        case "l":
          e.preventDefault();
          v.currentTime = Math.min(v.duration, v.currentTime + (e.key === "l" ? 10 : 5));
          break;
        case "arrowup":
          e.preventDefault();
          v.volume = Math.min(1, v.volume + 0.1);
          setVolume(v.volume);
          break;
        case "arrowdown":
          e.preventDefault();
          v.volume = Math.max(0, v.volume - 0.1);
          setVolume(v.volume);
          break;
        case "p":
          if (e.shiftKey && pipSupported) {
            e.preventDefault();
            togglePiP();
          }
          break;
      }
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [pipSupported]);

  const toggleFullscreen = useCallback(async () => {
    const el = containerRef.current;
    if (!el) return;
    if (document.fullscreenElement) {
      await document.exitFullscreen();
    } else {
      await el.requestFullscreen();
    }
  }, []);

  const togglePiP = useCallback(async () => {
    const v = videoRef.current;
    if (!v) return;
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else {
        await v.requestPictureInPicture();
      }
    } catch (err) {
      console.warn("PiP failed:", err);
    }
  }, []);

  useEffect(() => {
    const handleFsChange = () => setFullscreen(!!document.fullscreenElement);
    document.addEventListener("fullscreenchange", handleFsChange);
    return () => document.removeEventListener("fullscreenchange", handleFsChange);
  }, []);

  // Auto-hide controls
  const showControlsTemporarily = useCallback(() => {
    setShowControls(true);
    clearTimeout(hideTimeout.current);
    hideTimeout.current = setTimeout(() => {
      if (videoRef.current && !videoRef.current.paused) {
        setShowControls(false);
      }
    }, 3000);
  }, []);

  // Video events
  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;

    const onPlay = () => { setPlaying(true); showControlsTemporarily(); };
    const onPause = () => { setPlaying(false); setShowControls(true); };
    const onTime = () => setCurrent(v.currentTime);
    const onDuration = () => setDuration(v.duration);
    const onProgress = () => {
      if (v.buffered.length > 0) {
        setBuffered(v.buffered.end(v.buffered.length - 1));
      }
    };
    const onEnd = () => { setPlaying(false); onEnded?.(); };

    v.addEventListener("play", onPlay);
    v.addEventListener("pause", onPause);
    v.addEventListener("timeupdate", onTime);
    v.addEventListener("loadedmetadata", onDuration);
    v.addEventListener("progress", onProgress);
    v.addEventListener("ended", onEnd);

    return () => {
      v.removeEventListener("play", onPlay);
      v.removeEventListener("pause", onPause);
      v.removeEventListener("timeupdate", onTime);
      v.removeEventListener("loadedmetadata", onDuration);
      v.removeEventListener("progress", onProgress);
      v.removeEventListener("ended", onEnd);
    };
  }, [onEnded, showControlsTemporarily]);

  const togglePlay = () => {
    const v = videoRef.current;
    if (!v) return;
    v.paused ? v.play() : v.pause();
  };

  const handleSeek = (e: React.MouseEvent<HTMLDivElement>) => {
    const v = videoRef.current;
    const bar = progressRef.current;
    if (!v || !bar) return;
    const rect = bar.getBoundingClientRect();
    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    v.currentTime = pct * v.duration;
  };

  const handleSpeedChange = (rate: number) => {
    const v = videoRef.current;
    if (!v) return;
    v.playbackRate = rate;
    setPlaybackRate(rate);
    setShowSpeed(false);
  };

  const fmt = (s: number) => {
    if (!isFinite(s)) return "0:00";
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = Math.floor(s % 60);
    if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${sec.toString().padStart(2, "0")}`;
    return `${m}:${sec.toString().padStart(2, "0")}`;
  };

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;
  const bufferedPct = duration > 0 ? (buffered / duration) * 100 : 0;

  return (
    <div
      ref={containerRef}
      className="relative aspect-video bg-black md:rounded-xl overflow-hidden group"
      onMouseMove={showControlsTemporarily}
      onMouseLeave={() => playing && setShowControls(false)}
      onClick={(e) => {
        if ((e.target as HTMLElement).closest("[data-controls]")) return;
        // Mobile: tap toggles controls visibility. Desktop: tap toggles play.
        if ("ontouchstart" in window) {
          setShowControls(prev => !prev);
        } else {
          togglePlay();
        }
      }}
    >
      {/* Blurred background fill for vertical/non-16:9 videos */}
      {(poster || src) && (
        <div className="absolute inset-0 overflow-hidden">
          {poster ? (
            <div
              className="absolute inset-[-20px] bg-cover bg-center blur-3xl opacity-60 saturate-150"
              style={{ backgroundImage: `url(${poster})` }}
            />
          ) : (
            <video
              src={isHlsUrl(src) ? undefined : src}
              className="absolute inset-[-20px] w-[calc(100%+40px)] h-[calc(100%+40px)] object-cover blur-3xl opacity-50 saturate-150"
              muted
              playsInline
              preload="metadata"
            />
          )}
        </div>
      )}
      <video
        ref={videoRef}
        src={isHlsUrl(src) ? undefined : src}
        poster={poster}
        autoPlay={isHlsUrl(src) ? false : autoPlay}
        playsInline
        className="relative w-full h-full object-contain z-[1]"
      />

      {/* Center play button when paused */}
      {!playing && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/30 transition-opacity">
          <div className="w-16 h-16 md:w-20 md:h-20 rounded-full bg-primary/90 flex items-center justify-center backdrop-blur-sm">
            <Play className="w-8 h-8 md:w-10 md:h-10 text-primary-foreground ml-1" fill="currentColor" />
          </div>
        </div>
      )}

      {/* Controls overlay */}
      <div
        data-controls
        className={`absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent pt-16 pb-2 px-2 md:px-3 transition-opacity duration-300 ${
          showControls ? "opacity-100" : "opacity-0 pointer-events-none"
        }`}
      >
        {/* Progress bar */}
        <div
          ref={progressRef}
          onClick={handleSeek}
          className="w-full h-3 md:h-2 group/progress cursor-pointer flex items-center mb-1.5"
        >
          <div className="relative w-full h-1 group-hover/progress:h-1.5 bg-white/20 rounded-full transition-all">
            <div
              className="absolute inset-y-0 left-0 bg-white/30 rounded-full"
              style={{ width: `${bufferedPct}%` }}
            />
            <div
              className="absolute inset-y-0 left-0 bg-primary rounded-full"
              style={{ width: `${progress}%` }}
            />
            <div
              className="absolute top-1/2 -translate-y-1/2 w-3.5 h-3.5 md:w-3 md:h-3 bg-primary rounded-full opacity-0 group-hover/progress:opacity-100 transition-opacity"
              style={{ left: `${progress}%`, transform: `translateX(-50%) translateY(-50%)` }}
            />
          </div>
        </div>

        {/* Controls row */}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-1 md:gap-2">
            <button onClick={togglePlay} className="p-1.5 hover:bg-white/10 rounded-lg transition-colors">
              {playing ? (
                <Pause className="w-5 h-5 text-white" fill="currentColor" />
              ) : (
                <Play className="w-5 h-5 text-white ml-0.5" fill="currentColor" />
              )}
            </button>

            <button
              onClick={() => { const v = videoRef.current; if (v) v.currentTime -= 10; }}
              className="p-1.5 hover:bg-white/10 rounded-lg transition-colors"
            >
              <SkipBack className="w-4 h-4 text-white" />
            </button>
            <button
              onClick={() => { const v = videoRef.current; if (v) v.currentTime += 10; }}
              className="p-1.5 hover:bg-white/10 rounded-lg transition-colors"
            >
              <SkipForward className="w-4 h-4 text-white" />
            </button>

            {/* Volume */}
            <div className="hidden md:flex items-center gap-1 group/vol">
              <button
                onClick={() => {
                  const v = videoRef.current;
                  if (v) { v.muted = !v.muted; setMuted(v.muted); }
                }}
                className="p-1.5 hover:bg-white/10 rounded-lg transition-colors"
              >
                {muted || volume === 0 ? (
                  <VolumeX className="w-4 h-4 text-white" />
                ) : (
                  <Volume2 className="w-4 h-4 text-white" />
                )}
              </button>
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={muted ? 0 : volume}
                onChange={(e) => {
                  const val = parseFloat(e.target.value);
                  const v = videoRef.current;
                  if (v) { v.volume = val; v.muted = val === 0; }
                  setVolume(val);
                  setMuted(val === 0);
                }}
                className="w-0 group-hover/vol:w-16 transition-all duration-200 accent-primary h-1 cursor-pointer"
              />
            </div>

            <span className="text-xs text-white/80 ml-1 tabular-nums">
              {fmt(currentTime)} / {fmt(duration)}
            </span>
          </div>

          <div className="flex items-center gap-1">
            {/* Speed */}
            <div className="relative">
              <button
                onClick={() => setShowSpeed(!showSpeed)}
                className="p-1.5 hover:bg-white/10 rounded-lg transition-colors text-xs text-white font-medium min-w-[36px]"
              >
                {playbackRate === 1 ? <Settings className="w-4 h-4 text-white" /> : `${playbackRate}x`}
              </button>
              {showSpeed && (
                <div className="absolute bottom-full mb-2 right-0 bg-background/95 backdrop-blur-md border border-border rounded-lg p-1 z-10 min-w-[80px]">
                  {[0.5, 0.75, 1, 1.25, 1.5, 2].map((r) => (
                    <button
                      key={r}
                      onClick={() => handleSpeedChange(r)}
                      className={`block w-full text-left px-3 py-1.5 text-xs rounded transition-colors ${
                        playbackRate === r ? "text-primary bg-primary/10" : "text-foreground hover:bg-secondary"
                      }`}
                    >
                      {r}x
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* PiP */}
            {pipSupported && (
              <button onClick={togglePiP} className="p-1.5 hover:bg-white/10 rounded-lg transition-colors">
                <PictureInPicture2 className="w-4 h-4 text-white" />
              </button>
            )}

            {/* Fullscreen */}
            <button onClick={toggleFullscreen} className="p-2 hover:bg-white/10 rounded-lg transition-colors">
              {fullscreen ? (
                <Minimize className="w-5 h-5 text-white" />
              ) : (
                <Maximize className="w-5 h-5 text-white" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile: Tap zones for ±10s seek */}
      <div className="absolute inset-0 flex md:hidden pointer-events-none">
        <div
          className="w-1/3 h-full pointer-events-auto"
          onDoubleClick={() => { const v = videoRef.current; if (v) v.currentTime -= 10; }}
        />
        <div className="w-1/3 h-full" />
        <div
          className="w-1/3 h-full pointer-events-auto"
          onDoubleClick={() => { const v = videoRef.current; if (v) v.currentTime += 10; }}
        />
      </div>
    </div>
  );
};

export default VideoPlayer;
