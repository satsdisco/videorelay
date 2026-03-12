import { useState } from "react";
import Header from "@/components/Header";
import Sidebar from "@/components/Sidebar";
import CategoryBar from "@/components/CategoryBar";
import VideoCard, { type VideoData } from "@/components/VideoCard";

import thumb1 from "@/assets/thumb-1.jpg";
import thumb2 from "@/assets/thumb-2.jpg";
import thumb3 from "@/assets/thumb-3.jpg";
import thumb4 from "@/assets/thumb-4.jpg";
import thumb5 from "@/assets/thumb-5.jpg";
import thumb6 from "@/assets/thumb-6.jpg";
import thumb7 from "@/assets/thumb-7.jpg";
import thumb8 from "@/assets/thumb-8.jpg";

const mockVideos: VideoData[] = [
  {
    id: "1",
    thumbnail: thumb1,
    title: "Lightning Network Explained: How Bitcoin Scales to Millions",
    creator: "Satoshi Academy",
    creatorNpub: "npub1abc...",
    views: "142K",
    zaps: "2.1M",
    duration: "18:42",
    timeAgo: "2 days ago",
  },
  {
    id: "2",
    thumbnail: thumb2,
    title: "Building a Decentralized Social Graph with Nostr Relays",
    creator: "Protocol Dev",
    creatorNpub: "npub1def...",
    views: "89K",
    zaps: "890K",
    duration: "32:15",
    timeAgo: "5 hours ago",
  },
  {
    id: "3",
    thumbnail: thumb3,
    title: "Home Mining Setup 2026: Complete Guide to Sovereign Computing",
    creator: "Mining Max",
    creatorNpub: "npub1ghi...",
    views: "256K",
    zaps: "4.2M",
    duration: "45:30",
    timeAgo: "1 week ago",
  },
  {
    id: "4",
    thumbnail: thumb4,
    title: "NIP-90 Deep Dive: Data Vending Machines on Nostr",
    creator: "NostrDev",
    creatorNpub: "npub1jkl...",
    views: "34K",
    zaps: "450K",
    duration: "28:11",
    timeAgo: "3 days ago",
  },
  {
    id: "5",
    thumbnail: thumb5,
    title: "Relay Architecture: Scaling Nostr for the Next Billion Users",
    creator: "Relay Runner",
    creatorNpub: "npub1mno...",
    views: "67K",
    zaps: "1.5M",
    duration: "22:08",
    timeAgo: "12 hours ago",
  },
  {
    id: "6",
    thumbnail: thumb6,
    title: "The Bitcoin Standard Podcast #247 - Hyperbitcoinization Timeline",
    creator: "BTC Podcast",
    creatorNpub: "npub1pqr...",
    views: "312K",
    zaps: "8.7M",
    duration: "1:24:33",
    timeAgo: "1 day ago",
    live: true,
  },
  {
    id: "7",
    thumbnail: thumb7,
    title: "Self-Custodial Wallets: Your Keys, Your Bitcoin, Your Freedom",
    creator: "Wallet Wizard",
    creatorNpub: "npub1stu...",
    views: "198K",
    zaps: "3.1M",
    duration: "15:47",
    timeAgo: "4 days ago",
  },
  {
    id: "8",
    thumbnail: thumb8,
    title: "Market Analysis: On-Chain Metrics Every Bitcoiner Should Know",
    creator: "Chain Analyst",
    creatorNpub: "npub1vwx...",
    views: "76K",
    zaps: "920K",
    duration: "41:22",
    timeAgo: "6 hours ago",
  },
];

const Index = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

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
          <CategoryBar />
        </div>

        <div className="px-6 pb-8">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-x-4 gap-y-8">
            {mockVideos.map((video) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>
        </div>
      </main>
    </div>
  );
};

export default Index;
