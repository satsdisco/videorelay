import { useState } from "react";
import { ArrowLeft, Radio, Users, Loader2, Send, MessageSquare } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useLiveStreams, useLiveChat, type LiveStream } from "@/hooks/useLiveStreams";
import { useRelayStore } from "@/hooks/useRelayStore";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import { getPool, DEFAULT_RELAYS, timeAgo } from "@/lib/nostr";
import { LIVE_CHAT_KIND, LIVE_EVENT_KIND } from "@/hooks/useLiveStreams";
import Header from "@/components/Header";

const StreamCard = ({ stream, onClick }: { stream: LiveStream; onClick: () => void }) => {
  const { profile } = useNostrProfile(stream.pubkey);
  const displayName = profile?.displayName || profile?.name || stream.pubkey.slice(0, 12) + "...";

  return (
    <div onClick={onClick} className="group cursor-pointer">
      <div className="relative aspect-video rounded-xl overflow-hidden bg-secondary mb-3">
        {stream.thumbnailUrl ? (
          <img src={stream.thumbnailUrl} alt={stream.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-red-500/20 to-primary/20 flex items-center justify-center">
            <Radio className="w-12 h-12 text-red-500 animate-pulse" />
          </div>
        )}
        <div className="absolute top-2 left-2 flex items-center gap-1.5 px-2 py-1 bg-red-600 rounded text-xs font-bold text-white">
          <span className="w-2 h-2 bg-white rounded-full animate-pulse" />
          LIVE
        </div>
        {stream.viewers > 0 && (
          <div className="absolute bottom-2 right-2 flex items-center gap-1 px-2 py-1 bg-background/80 rounded text-xs text-foreground backdrop-blur-sm">
            <Users className="w-3 h-3" />
            {stream.viewers}
          </div>
        )}
      </div>
      <h3 className="text-sm font-semibold text-foreground line-clamp-2 group-hover:text-primary transition-colors">{stream.title}</h3>
      <p className="text-xs text-muted-foreground mt-1">{displayName}</p>
      {stream.tags.length > 0 && (
        <div className="flex gap-1 mt-1 flex-wrap">
          {stream.tags.slice(0, 3).map(t => (
            <span key={t} className="text-[10px] text-primary/70 bg-primary/10 px-1.5 py-0.5 rounded">#{t}</span>
          ))}
        </div>
      )}
    </div>
  );
};

const LiveChatPanel = ({ streamId }: { streamId: string }) => {
  const { activeRelays } = useRelayStore();
  const { messages } = useLiveChat(streamId, activeRelays);
  const { isLoggedIn } = useNostrAuth();
  const [chatInput, setChatInput] = useState("");
  const [sending, setSending] = useState(false);

  const sendMessage = async () => {
    if (!chatInput.trim() || !window.nostr) return;
    setSending(true);
    try {
      const event = {
        kind: LIVE_CHAT_KIND,
        content: chatInput.trim(),
        tags: [["a", `${LIVE_EVENT_KIND}:${streamId}`]],
        created_at: Math.floor(Date.now() / 1000),
      };
      const signed = await (window.nostr as any).signEvent(event);
      const pool = getPool();
      await Promise.allSettled(pool.publish(DEFAULT_RELAYS, signed));
      setChatInput("");
    } catch (err) {
      console.error("Failed to send chat:", err);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="flex flex-col h-full bg-card rounded-xl border border-border">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-border">
        <MessageSquare className="w-4 h-4 text-primary" />
        <span className="text-sm font-semibold text-foreground">Live Chat</span>
        <span className="text-xs text-muted-foreground ml-auto">{messages.length} messages</span>
      </div>
      <div className="flex-1 overflow-y-auto px-4 py-2 space-y-2 min-h-0">
        {messages.map(msg => (
          <ChatBubble key={msg.id} pubkey={msg.pubkey} content={msg.content} createdAt={msg.createdAt} />
        ))}
        {messages.length === 0 && (
          <p className="text-xs text-muted-foreground text-center py-8">No messages yet...</p>
        )}
      </div>
      {isLoggedIn ? (
        <div className="flex gap-2 px-4 py-3 border-t border-border">
          <input
            value={chatInput}
            onChange={e => setChatInput(e.target.value)}
            onKeyDown={e => e.key === "Enter" && sendMessage()}
            placeholder="Say something..."
            className="flex-1 bg-secondary rounded-lg px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground outline-none border border-border focus:border-primary/50"
          />
          <button onClick={sendMessage} disabled={!chatInput.trim() || sending} className="p-2 bg-primary text-primary-foreground rounded-lg disabled:opacity-40">
            <Send className="w-4 h-4" />
          </button>
        </div>
      ) : (
        <p className="text-xs text-muted-foreground text-center py-3 border-t border-border">Sign in to chat</p>
      )}
    </div>
  );
};

const ChatBubble = ({ pubkey, content, createdAt }: { pubkey: string; content: string; createdAt: number }) => {
  const { profile } = useNostrProfile(pubkey);
  const name = profile?.displayName || profile?.name || pubkey.slice(0, 8) + "...";
  return (
    <div className="text-xs">
      <span className="font-semibold text-primary">{name}</span>
      <span className="text-muted-foreground ml-1">{timeAgo(createdAt)}</span>
      <p className="text-foreground mt-0.5">{content}</p>
    </div>
  );
};

const Live = () => {
  const navigate = useNavigate();
  const { activeRelays } = useRelayStore();
  const { streams, loading } = useLiveStreams(activeRelays);
  const [selectedStream, setSelectedStream] = useState<LiveStream | null>(null);

  if (selectedStream) {
    return (
      <div className="min-h-screen bg-background pb-16 md:pb-0">
        <div className="sticky top-0 z-50 flex items-center gap-3 px-4 py-3 bg-background/95 backdrop-blur-sm border-b border-border">
          <button onClick={() => setSelectedStream(null)} className="p-2 rounded-lg hover:bg-secondary transition-colors">
            <ArrowLeft className="w-5 h-5 text-foreground" />
          </button>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
            <h1 className="text-sm font-medium text-foreground truncate">{selectedStream.title}</h1>
          </div>
        </div>
        <div className="flex flex-col lg:flex-row gap-4 p-4 max-w-[1600px] mx-auto">
          <div className="flex-1">
            <div className="aspect-video bg-black rounded-xl overflow-hidden">
              <video src={selectedStream.streamingUrl} controls autoPlay playsInline className="w-full h-full" />
            </div>
          </div>
          <div className="w-full lg:w-[360px] h-[400px] lg:h-[calc(100vh-120px)]">
            <LiveChatPanel streamId={`${selectedStream.pubkey}:${selectedStream.dTag}`} />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-16 md:pb-0">
      <Header onMenuClick={() => {}} onSearch={() => {}} />
      <div className="max-w-[1600px] mx-auto px-4 py-6">
        <div className="flex items-center gap-3 mb-6">
          <Radio className="w-6 h-6 text-red-500" />
          <h1 className="text-xl font-bold text-foreground">Live Now</h1>
          {streams.length > 0 && (
            <span className="px-2 py-0.5 bg-red-600 text-white text-xs font-bold rounded-full">{streams.length}</span>
          )}
        </div>

        {loading ? (
          <div className="flex items-center gap-2 py-12 justify-center text-muted-foreground">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="text-sm">Scanning relays for live streams...</span>
          </div>
        ) : streams.length === 0 ? (
          <div className="text-center py-16 max-w-md mx-auto">
            <Radio className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
            <h2 className="text-lg font-semibold text-foreground mb-2">No one is live right now</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Live streams use NIP-53 (kind 30311) events. When creators go live on Nostr, their streams will appear here automatically.
            </p>
            <p className="text-xs text-muted-foreground">
              Try streaming with <a href="https://zap.stream" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">zap.stream</a> — it broadcasts NIP-53 events that VideoRelay picks up.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {streams.map(stream => (
              <StreamCard key={stream.id} stream={stream} onClick={() => setSelectedStream(stream)} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Live;
