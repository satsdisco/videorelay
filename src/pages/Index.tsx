import { useState, useEffect } from "react";
import Header from "@/components/Header";
import Sidebar from "@/components/Sidebar";
import CategoryBar from "@/components/CategoryBar";
import VideoCard from "@/components/VideoCard";
import { useNostrVideos } from "@/hooks/useNostrVideos";
import { Loader2, WifiOff, RefreshCw, Zap, TrendingUp, Clock } from "lucide-react";
import { getRandomLoadingMessage, getRandomEmptyMessage, getRandomErrorMessage } from "@/lib/loadingMessages";

// Fallback thumbnails for empty state
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

const Index = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [hashtag, setHashtag] = useState<string | undefined>(undefined);
  const [sortBy, setSortBy] = useState<"recent" | "popular">("recent");

  const { videos, loading, error, refetch } = useNostrVideos({
    limit: 40,
    hashtag,
    sortBy,
  });

  const handleCategoryChange = (category: string) => {
    if (category === "All") {
      setHashtag(undefined);
    } else {
      setHashtag(category.toLowerCase());
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Header onToggleSidebar={() => setSidebarCollapsed(!sidebarCollapsed)} />
      <Sidebar collapsed={sidebarCollapsed} />

      <main
        className={`pt-14 transition-all duration-300 ${
          sidebarCollapsed ? "ml-[72px]" : "ml-56"
        }`}
      >
        <div className="px-6 py-2">
          <CategoryBar onCategoryChange={handleCategoryChange} />
        </div>

        <div className="px-6 pb-8">
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

          {!loading && !error && videos.length === 0 && (
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
              <div className="flex items-center justify-between mb-4">
                <p className="text-xs text-muted-foreground">
                  {videos.length} videos from Nostr relays
                </p>
                <button
                  onClick={refetch}
                  className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-primary transition-colors"
                >
                  <RefreshCw className="w-3 h-3" />
                  Refresh
                </button>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-x-4 gap-y-8">
                {videos.map((video) => (
                  <VideoCard key={video.id} video={video} />
                ))}
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
};

export default Index;
