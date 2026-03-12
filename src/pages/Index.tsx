import { useState, useEffect, useRef, useMemo } from "react";
import Header from "@/components/Header";
import Sidebar, { type SidebarView } from "@/components/Sidebar";
import RelayManager from "@/components/RelayManager";
import CategoryBar from "@/components/CategoryBar";
import VideoCard from "@/components/VideoCard";
import ShortCard from "@/components/ShortCard";
import { useNostrVideos } from "@/hooks/useNostrVideos";
import { useRelayStore } from "@/hooks/useRelayStore";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import { Loader2, WifiOff, RefreshCw, Zap, TrendingUp, Clock, ChevronLeft, ChevronRight, Flame, Users } from "lucide-react";
import { getRandomLoadingMessage, getRandomEmptyMessage, getRandomErrorMessage } from "@/lib/loadingMessages";

import thumb1 from "@/assets/thumb-1.jpg";
import thumb2 from "@/assets/thumb-2.jpg";

const LoadingState = () => {
  const [message, setMessage] = useState(getRandomLoadingMessage);

  useEffect(() => {
    const interval = setInterval(() => {
      setMessage(getRandomLoadingMessage());
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="flex flex-col items-center justify-center py-20">
      <Loader2 className="w-8 h-8 text-primary animate-spin mb-4" />
      <p className="text-foreground font-medium mb-1">Hang tight, anon...</p>
      <p className="text-muted-foreground text-sm text-center max-w-md transition-all duration-500">
        {message}
      </p>
    </div>
  );
};

const ShortsShelf = ({ shorts }: { shorts: import("@/lib/nostr").ParsedVideo[] }) => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);

  const checkScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    setCanScrollLeft(el.scrollLeft > 0);
    setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 10);
  };

  const scroll = (dir: "left" | "right") => {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollBy({ left: dir === "left" ? -400 : 400, behavior: "smooth" });
  };

  if (shorts.length === 0) return null;

  return (
    <div className="mb-8">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <Flame className="w-5 h-5 text-primary" />
          <h2 className="text-base font-bold text-foreground">Shorts</h2>
          <span className="text-xs text-muted-foreground">({shorts.length})</span>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={() => scroll("left")}
            disabled={!canScrollLeft}
            className="p-1.5 rounded-full bg-secondary hover:bg-muted transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="w-4 h-4 text-foreground" />
          </button>
          <button
            onClick={() => scroll("right")}
            disabled={!canScrollRight}
            className="p-1.5 rounded-full bg-secondary hover:bg-muted transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <ChevronRight className="w-4 h-4 text-foreground" />
          </button>
        </div>
      </div>
      <div
        ref={scrollRef}
        onScroll={checkScroll}
        className="flex gap-3 overflow-x-auto scrollbar-hide pb-2"
      >
        {shorts.map((video) => (
          <ShortCard key={video.id} video={video} />
        ))}
      </div>
    </div>
  );
};

const Index = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [hashtag, setHashtag] = useState<string | undefined>(undefined);
  const [sortBy, setSortBy] = useState<"recent" | "popular">("recent");
  const [searchQuery, setSearchQuery] = useState("");
  const [activeView, setActiveView] = useState<SidebarView>("home");
  const [relayManagerOpen, setRelayManagerOpen] = useState(false);

  const { activeRelays } = useRelayStore();
  const { pubkey } = useNostrAuth();

  // Map sidebar view to fetch options
  const fetchOptions = useMemo(() => {
    const base = { relays: activeRelays, limit: 60, hashtag, sortBy: sortBy as "recent" | "popular" };

    switch (activeView) {
      case "trending":
        return { ...base, sortBy: "popular" as const };
      case "zapped":
        return { ...base, sortBy: "popular" as const };
      case "following":
        return { ...base, authors: pubkey ? [pubkey] : undefined };
      default:
        return base;
    }
  }, [activeView, activeRelays, hashtag, sortBy, pubkey]);

  const { videos, loading, error, refetch } = useNostrVideos(fetchOptions);

  // Handle sidebar view changes
  const handleViewChange = (view: SidebarView) => {
    setActiveView(view);
    if (view === "trending" || view === "zapped") {
      setSortBy("popular");
    } else if (view === "home") {
      setSortBy("recent");
    }
  };

  const { shorts, longForm } = useMemo(() => {
    let filtered = videos;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      filtered = videos.filter(
        (v) =>
          v.title.toLowerCase().includes(q) ||
          v.summary.toLowerCase().includes(q) ||
          v.tags.some((t) => t.toLowerCase().includes(q))
      );
    }
    const shorts = filtered.filter((v) => v.isShort);
    const longForm = filtered.filter((v) => !v.isShort);
    return { shorts, longForm };
  }, [videos, searchQuery]);

  const handleCategoryChange = (category: string) => {
    if (category === "All") {
      setHashtag(undefined);
    } else {
      setHashtag(category.toLowerCase());
    }
  };

  const viewTitle = {
    home: null,
    trending: "Trending",
    zapped: "Most Zapped",
    following: "Following",
    live: "Live",
  }[activeView];

  return (
    <div className="min-h-screen bg-background">
      <Header onToggleSidebar={() => setSidebarCollapsed(!sidebarCollapsed)} onSearch={setSearchQuery} />
      <Sidebar
        collapsed={sidebarCollapsed}
        activeView={activeView}
        onChangeView={handleViewChange}
        onOpenRelays={() => setRelayManagerOpen(true)}
      />
      <RelayManager open={relayManagerOpen} onClose={() => setRelayManagerOpen(false)} />

      <main
        className={`pt-14 transition-all duration-300 ${
          sidebarCollapsed ? "ml-[72px]" : "ml-56"
        }`}
      >
        <div className="px-6 py-2">
          <CategoryBar onCategoryChange={handleCategoryChange} />
        </div>

        <div className="px-6 pb-8">
          {/* View header */}
          {viewTitle && (
            <div className="flex items-center gap-2 mb-4">
              {activeView === "following" && <Users className="w-5 h-5 text-primary" />}
              {activeView === "trending" && <TrendingUp className="w-5 h-5 text-primary" />}
              {activeView === "zapped" && <Zap className="w-5 h-5 text-primary" />}
              <h1 className="text-xl font-bold text-foreground">{viewTitle}</h1>
            </div>
          )}

          {/* Following requires login */}
          {activeView === "following" && !pubkey && (
            <div className="flex flex-col items-center justify-center py-20">
              <Users className="w-10 h-10 text-muted-foreground mb-3" />
              <p className="text-foreground font-medium mb-1">Sign in to see who you follow</p>
              <p className="text-muted-foreground text-sm">Connect your Nostr extension to view content from people you follow.</p>
            </div>
          )}

          {loading && <LoadingState />}

          {error && (
            <div className="flex flex-col items-center justify-center py-20">
              <WifiOff className="w-10 h-10 text-muted-foreground mb-3" />
              <p className="text-foreground font-medium mb-1">Oops.</p>
              <p className="text-muted-foreground text-sm mb-4 text-center max-w-md">{getRandomErrorMessage()}</p>
              <button
                onClick={refetch}
                className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
              >
                <RefreshCw className="w-4 h-4" />
                Try Again
              </button>
            </div>
          )}

          {!loading && !error && videos.length === 0 && !(activeView === "following" && !pubkey) && (
            <div className="flex flex-col items-center justify-center py-20">
              <Zap className="w-10 h-10 text-primary/40 mb-3" />
              <p className="text-foreground font-medium mb-1">{getRandomEmptyMessage()}</p>
              <p className="text-muted-foreground/60 text-sm mb-4">
                {hashtag
                  ? `No videos tagged #${hashtag}. The relays have spoken.`
                  : "Try a different category or check back later."}
              </p>
              <button
                onClick={() => {
                  setHashtag(undefined);
                  refetch();
                }}
                className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
              >
                <RefreshCw className="w-4 h-4" />
                Refresh
              </button>
            </div>
          )}

          {!loading && !error && videos.length > 0 && (
            <>
              {/* Long-form videos — the main content */}
              <div className="flex items-center justify-between mb-4">
                <p className="text-xs text-muted-foreground">
                  {longForm.length} videos from {activeRelays.length} relays
                </p>
                <div className="flex items-center gap-3">
                  <div className="flex items-center bg-secondary rounded-full p-0.5">
                    <button
                      onClick={() => setSortBy("recent")}
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        sortBy === "recent" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
                      }`}
                    >
                      <Clock className="w-3 h-3" />
                      Recent
                    </button>
                    <button
                      onClick={() => setSortBy("popular")}
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        sortBy === "popular" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
                      }`}
                    >
                      <TrendingUp className="w-3 h-3" />
                      Popular
                    </button>
                  </div>
                  <button
                    onClick={refetch}
                    className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-primary transition-colors"
                  >
                    <RefreshCw className="w-3 h-3" />
                    Refresh
                  </button>
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-x-4 gap-y-8">
                {longForm.map((video) => (
                  <VideoCard key={video.id} video={video} />
                ))}
              </div>

              {/* Shorts — tucked at the bottom as a bonus section */}
              {shorts.length > 0 && (
                <div className="mt-10 pt-6 border-t border-border">
                  <ShortsShelf shorts={shorts} />
                </div>
              )}
            </>
          )}
        </div>
      </main>
    </div>
  );
};

export default Index;
