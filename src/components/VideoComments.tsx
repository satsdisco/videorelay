import { useState, useEffect, useCallback } from "react";
import { MessageSquare, Send, Loader2 } from "lucide-react";
import { getPool, DEFAULT_RELAYS, timeAgo } from "@/lib/nostr";
import { useNostrProfile } from "@/hooks/useNostrProfile";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import type { Event } from "nostr-tools";

interface Comment {
  id: string;
  pubkey: string;
  content: string;
  createdAt: number;
}

const CommentItem = ({ comment }: { comment: Comment }) => {
  const { profile } = useNostrProfile(comment.pubkey);
  const displayName = profile?.displayName || profile?.name || comment.pubkey.slice(0, 12) + "...";
  const avatar = profile?.picture;

  return (
    <div className="flex gap-3 py-3">
      {avatar ? (
        <img src={avatar} alt="" className="w-8 h-8 rounded-full object-cover shrink-0" />
      ) : (
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary/80 to-accent flex items-center justify-center text-xs font-bold text-primary-foreground shrink-0">
          {displayName[0]?.toUpperCase() || "?"}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold text-foreground">{displayName}</span>
          <span className="text-xs text-muted-foreground">{timeAgo(comment.createdAt)}</span>
        </div>
        <p className="text-sm text-secondary-foreground mt-0.5 whitespace-pre-wrap break-words">
          {comment.content}
        </p>
      </div>
    </div>
  );
};

interface VideoCommentsProps {
  videoId: string;
}

const VideoComments = ({ videoId }: VideoCommentsProps) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState("");
  const [posting, setPosting] = useState(false);
  const { isLoggedIn } = useNostrAuth();

  const fetchComments = useCallback(async () => {
    setLoading(true);
    try {
      const pool = getPool();
      // Fetch both NIP-22 comments (kind 1111) and legacy kind 1 replies
      const events: Event[] = await pool.querySync(DEFAULT_RELAYS, {
        kinds: [1111, 1],
        "#e": [videoId],
        limit: 50,
      });

      const parsed: Comment[] = events
        .map((e) => ({
          id: e.id,
          pubkey: e.pubkey,
          content: e.content,
          createdAt: e.created_at,
        }))
        .sort((a, b) => b.createdAt - a.createdAt);

      setComments(parsed);
    } catch (err) {
      console.error("Failed to fetch comments:", err);
    } finally {
      setLoading(false);
    }
  }, [videoId]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handlePost = async () => {
    if (!newComment.trim() || !window.nostr) return;
    setPosting(true);

    try {
      // NIP-22: kind 1111 comment with proper root marker
      const event = {
        kind: 1111,
        content: newComment.trim(),
        tags: [
          ["e", videoId, "", "root"],
          ["k", "34235"],
        ],
        created_at: Math.floor(Date.now() / 1000),
      };

      const signedEvent = await (window.nostr as any).signEvent(event);
      const pool = getPool();
      const pubs = pool.publish(DEFAULT_RELAYS, signedEvent);
      await Promise.allSettled(pubs);

      setNewComment("");
      setComments((prev) => [
        {
          id: signedEvent.id,
          pubkey: signedEvent.pubkey,
          content: signedEvent.content,
          createdAt: signedEvent.created_at,
        },
        ...prev,
      ]);
    } catch (err) {
      console.error("Failed to post comment:", err);
    } finally {
      setPosting(false);
    }
  };

  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <MessageSquare className="w-5 h-5 text-foreground" />
        <h3 className="text-base font-bold text-foreground">
          {comments.length} Comment{comments.length !== 1 ? "s" : ""}
        </h3>
      </div>

      {/* Comment input */}
      {isLoggedIn ? (
        <div className="flex gap-3 mb-6">
          <input
            type="text"
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handlePost()}
            placeholder="Add a comment..."
            className="flex-1 bg-secondary rounded-lg px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none border border-border focus:border-primary/50 transition-colors"
          />
          <button
            onClick={handlePost}
            disabled={!newComment.trim() || posting}
            className="px-4 py-2.5 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {posting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
          </button>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground mb-6">
          Sign in with your Nostr extension to comment.
        </p>
      )}

      {/* Comments list */}
      {loading ? (
        <div className="flex items-center gap-2 py-4 text-muted-foreground">
          <Loader2 className="w-4 h-4 animate-spin" />
          <span className="text-sm">Loading comments from relays...</span>
        </div>
      ) : comments.length === 0 ? (
        <p className="text-sm text-muted-foreground py-4">
          No comments yet. Be the first to speak, anon.
        </p>
      ) : (
        <div className="divide-y divide-border">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} />
          ))}
        </div>
      )}
    </div>
  );
};

export default VideoComments;
