import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Upload as UploadIcon, Send, X, Plus } from "lucide-react";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import { getPool, ADDRESSABLE_VIDEO_KIND } from "@/lib/nostr";
import { useRelayStore } from "@/hooks/useRelayStore";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";

const Upload = () => {
  const navigate = useNavigate();
  const { isLoggedIn, pubkey } = useNostrAuth();
  const { activeRelays } = useRelayStore();
  const { toast } = useToast();

  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [videoUrl, setVideoUrl] = useState("");
  const [thumbnailUrl, setThumbnailUrl] = useState("");
  const [tagInput, setTagInput] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [publishing, setPublishing] = useState(false);

  const addTag = () => {
    const t = tagInput.trim().toLowerCase().replace(/^#/, "");
    if (t && !tags.includes(t)) {
      setTags([...tags, t]);
      setTagInput("");
    }
  };

  const removeTag = (tag: string) => setTags(tags.filter((t) => t !== tag));

  const handlePublish = async () => {
    if (!window.nostr) {
      toast({ title: "No Nostr extension", description: "Install Alby or nos2x to publish.", variant: "destructive" });
      return;
    }
    if (!title.trim() || !videoUrl.trim()) {
      toast({ title: "Missing fields", description: "Title and video URL are required.", variant: "destructive" });
      return;
    }

    setPublishing(true);
    try {
      const eventTags: string[][] = [
        ["title", title.trim()],
        ["url", videoUrl.trim()],
        ["d", `${Date.now()}`],
      ];
      if (thumbnailUrl.trim()) eventTags.push(["thumb", thumbnailUrl.trim()]);
      tags.forEach((t) => eventTags.push(["t", t]));

      const unsignedEvent = {
        kind: ADDRESSABLE_VIDEO_KIND,
        created_at: Math.floor(Date.now() / 1000),
        tags: eventTags,
        content: summary.trim(),
      };

      const signedEvent = await window.nostr.signEvent(unsignedEvent);
      const pool = getPool();

      await Promise.any(
        activeRelays.map((relay) =>
          pool.publish([relay], signedEvent as any)
        )
      );

      toast({ title: "Published! ⚡", description: "Your video event has been broadcast to relays." });
      navigate("/");
    } catch (err: any) {
      console.error("Publish failed:", err);
      toast({ title: "Publish failed", description: err?.message || "Could not publish to relays.", variant: "destructive" });
    } finally {
      setPublishing(false);
    }
  };

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center max-w-md px-4">
          <UploadIcon className="w-12 h-12 text-primary mx-auto mb-4" />
          <h2 className="text-xl font-bold text-foreground mb-2">Sign in to publish</h2>
          <p className="text-muted-foreground text-sm mb-4">You need a Nostr extension (NIP-07) to publish videos.</p>
          <Button onClick={() => navigate("/")} variant="outline">Back to Home</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="sticky top-0 z-50 flex items-center gap-3 px-4 py-3 bg-background/95 backdrop-blur-sm border-b border-border">
        <button onClick={() => navigate("/")} className="p-2 rounded-lg hover:bg-secondary transition-colors">
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </button>
        <h1 className="text-sm font-medium text-foreground">Publish a Video</h1>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="space-y-6">
          <div>
            <label className="text-sm font-medium text-foreground mb-2 block">Title *</label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Give your video a title" className="bg-secondary" />
          </div>

          <div>
            <label className="text-sm font-medium text-foreground mb-2 block">Video URL *</label>
            <Input value={videoUrl} onChange={(e) => setVideoUrl(e.target.value)} placeholder="https://... (.mp4, .webm, .m3u8)" className="bg-secondary" />
            <p className="text-xs text-muted-foreground mt-1">Paste a direct link to your video file hosted on any server or CDN.</p>
          </div>

          <div>
            <label className="text-sm font-medium text-foreground mb-2 block">Thumbnail URL</label>
            <Input value={thumbnailUrl} onChange={(e) => setThumbnailUrl(e.target.value)} placeholder="https://... (.jpg, .png, .webp)" className="bg-secondary" />
          </div>

          <div>
            <label className="text-sm font-medium text-foreground mb-2 block">Description</label>
            <Textarea value={summary} onChange={(e) => setSummary(e.target.value)} placeholder="What is this video about?" className="bg-secondary min-h-[100px]" />
          </div>

          <div>
            <label className="text-sm font-medium text-foreground mb-2 block">Tags</label>
            <div className="flex gap-2 mb-2">
              <Input
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addTag())}
                placeholder="Add a tag..."
                className="bg-secondary"
              />
              <Button variant="secondary" size="icon" onClick={addTag}><Plus className="w-4 h-4" /></Button>
            </div>
            {tags.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {tags.map((tag) => (
                  <span key={tag} className="flex items-center gap-1 px-3 py-1 bg-primary/10 text-primary text-xs rounded-full font-medium">
                    #{tag}
                    <button onClick={() => removeTag(tag)}><X className="w-3 h-3" /></button>
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Preview */}
          {videoUrl && (
            <div>
              <label className="text-sm font-medium text-foreground mb-2 block">Preview</label>
              <div className="aspect-video bg-secondary rounded-xl overflow-hidden">
                <video src={videoUrl} controls className="w-full h-full" poster={thumbnailUrl || undefined} />
              </div>
            </div>
          )}

          <Button onClick={handlePublish} disabled={publishing || !title.trim() || !videoUrl.trim()} className="w-full" size="lg">
            {publishing ? (
              <span className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-primary-foreground border-t-transparent rounded-full animate-spin" />
                Publishing...
              </span>
            ) : (
              <span className="flex items-center gap-2">
                <Send className="w-4 h-4" />
                Publish to Nostr
              </span>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Upload;
