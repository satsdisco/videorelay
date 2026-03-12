import { useState } from "react";
import Header from "@/components/Header";
import Sidebar from "@/components/Sidebar";
import CategoryBar from "@/components/CategoryBar";
import VideoCard from "@/components/VideoCard";
import { useNostrVideos } from "@/hooks/useNostrVideos";
import { Loader2, WifiOff, RefreshCw } from "lucide-react";

// Fallback thumbnails for empty state
import thumb1 from "@/assets/thumb-1.jpg";
import thumb2 from "@/assets/thumb-2.jpg";

const Index = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [hashtag, setHashtag] = useState<string | undefined>(undefined);

  const { videos, loading, error, refetch } = useNostrVideos({
    limit: 40,
    hashtag,
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
          {loading && (
            <div className="flex flex-col items-center justify-center py-20">
              <Loader2 className="w-8 h-8 text-primary animate-spin mb-3" />
              <p className="text-muted-foreground text-sm">Fetching videos from Nostr relays...</p>
              <p className="text-muted-foreground/60 text-xs mt-1">Connecting to {6} relays</p>
            </div>
          )}

          {error && (
            <div className="flex flex-col items-center justify-center py-20">
              <WifiOff className="w-10 h-10 text-muted-foreground mb-3" />
              <p className="text-muted-foreground mb-4">{error}</p>
              <button
                onClick={refetch}
                className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
              >
                <RefreshCw className="w-4 h-4" />
                Retry
              </button>
            </div>
          )}

          {!loading && !error && videos.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20">
              <p className="text-muted-foreground text-lg mb-2">No videos found</p>
              <p className="text-muted-foreground/60 text-sm mb-4">
                {hashtag
                  ? `No videos with #${hashtag} tag found on relays`
                  : "No video events found on connected relays"}
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
