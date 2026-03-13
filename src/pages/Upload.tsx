import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Upload as UploadIcon, Send, X, Plus, Check, Loader2, Film, Link } from "lucide-react";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import { getPool, ADDRESSABLE_VIDEO_KIND } from "@/lib/nostr";
import { useRelayStore } from "@/hooks/useRelayStore";
import { uploadToBlossom, validateVideoFile, type UploadProgress } from "@/lib/blossom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";

type UploadMode = "file" | "url";

const Upload = () => {
  const navigate = useNavigate();
  const { isLoggedIn } = useNostrAuth();
  const { activeRelays } = useRelayStore();
  const { toast } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [mode, setMode] = useState<UploadMode>("file");
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [videoUrl, setVideoUrl] = useState("");
  const [thumbnailUrl, setThumbnailUrl] = useState("");
  const [tagInput, setTagInput] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [publishing, setPublishing] = useState(false);

  // File upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<UploadProgress[]>([]);
  const [fileError, setFileError] = useState<string | null>(null);

  const addTag = () => {
    const t = tagInput.trim().toLowerCase().replace(/^#/, "");
    if (t && !tags.includes(t)) {
      setTags([...tags, t]);
      setTagInput("");
    }
  };

  const removeTag = (tag: string) => setTags(tags.filter((t) => t !== tag));

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setFileError(null);
    const error = validateVideoFile(file);
    if (error) {
      setFileError(error);
      return;
    }
    setSelectedFile(file);
    if (!title) setTitle(file.name.replace(/\.[^.]+$/, ""));
  };

  const handleFileUpload = async () => {
    if (!selectedFile) return;
    setUploading(true);
    setFileError(null);

    try {
      const { url } = await uploadToBlossom(selectedFile, setUploadProgress);
      setVideoUrl(url);
      toast({ title: "Upload complete ⚡", description: "Video uploaded to Blossom servers." });
    } catch (err: any) {
      setFileError(err?.message || "Upload failed");
      toast({ title: "Upload failed", description: err?.message, variant: "destructive" });
    } finally {
      setUploading(false);
    }
  };

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
      const publishResults = pool.publish(activeRelays, signedEvent as any);
      await Promise.allSettled(publishResults);

      toast({ title: "Published! ⚡", description: "Your video event has been broadcast to relays." });
      navigate("/");
    } catch (err: any) {
      toast({ title: "Publish failed", description: err?.message || "Could not publish to relays.", variant: "destructive" });
    } finally {
      setPublishing(false);
    }
  };

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen bg-background pb-16 md:pb-0 flex items-center justify-center">
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
    <div className="min-h-screen bg-background pb-16 md:pb-0">
      <div className="sticky top-0 z-50 flex items-center gap-3 px-4 py-3 bg-background/95 backdrop-blur-sm border-b border-border">
        <button onClick={() => navigate("/")} className="p-2 rounded-lg hover:bg-secondary transition-colors">
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </button>
        <h1 className="text-sm font-medium text-foreground">Publish a Video</h1>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="space-y-6">
          {/* Upload mode tabs */}
          <div className="flex bg-secondary rounded-lg p-1">
            <button
              onClick={() => setMode("file")}
              className={`flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-md text-sm font-medium transition-all ${
                mode === "file" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Film className="w-4 h-4" />
              Upload File
            </button>
            <button
              onClick={() => setMode("url")}
              className={`flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-md text-sm font-medium transition-all ${
                mode === "url" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Link className="w-4 h-4" />
              Paste URL
            </button>
          </div>

          {/* File upload section */}
          {mode === "file" && (
            <div>
              <label className="text-sm font-medium text-foreground mb-2 block">Video File *</label>

              {!selectedFile ? (
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="border-2 border-dashed border-border rounded-xl p-8 text-center cursor-pointer hover:border-primary/50 transition-colors"
                >
                  <UploadIcon className="w-10 h-10 text-muted-foreground mx-auto mb-3" />
                  <p className="text-sm text-foreground font-medium">Click to select a video</p>
                  <p className="text-xs text-muted-foreground mt-1">MP4, WebM, MOV — up to 500MB</p>
                </div>
              ) : (
                <div className="bg-secondary rounded-xl p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <Film className="w-5 h-5 text-primary shrink-0" />
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-foreground truncate">{selectedFile.name}</p>
                        <p className="text-xs text-muted-foreground">{(selectedFile.size / 1024 / 1024).toFixed(1)} MB</p>
                      </div>
                    </div>
                    {!uploading && !videoUrl && (
                      <button onClick={() => { setSelectedFile(null); setUploadProgress([]); }} className="p-1 hover:bg-background rounded">
                        <X className="w-4 h-4 text-muted-foreground" />
                      </button>
                    )}
                  </div>

                  {/* Server status */}
                  {uploadProgress.length > 0 && (
                    <div className="space-y-2 mb-3">
                      {uploadProgress.map((p) => (
                        <div key={p.server} className="flex items-center gap-2 text-xs">
                          {p.status === "success" ? (
                            <Check className="w-3.5 h-3.5 text-green-500" />
                          ) : p.status === "error" ? (
                            <X className="w-3.5 h-3.5 text-destructive" />
                          ) : p.status === "uploading" ? (
                            <Loader2 className="w-3.5 h-3.5 text-primary animate-spin" />
                          ) : (
                            <div className="w-3.5 h-3.5 rounded-full border border-muted-foreground" />
                          )}
                          <span className={p.status === "success" ? "text-green-500" : p.status === "error" ? "text-destructive" : "text-muted-foreground"}>
                            {p.server.replace("https://", "")}
                          </span>
                          {p.error && <span className="text-destructive">— {p.error}</span>}
                        </div>
                      ))}
                    </div>
                  )}

                  {!videoUrl && (
                    <Button onClick={handleFileUpload} disabled={uploading} className="w-full" size="sm">
                      {uploading ? (
                        <span className="flex items-center gap-2">
                          <Loader2 className="w-4 h-4 animate-spin" />
                          Uploading to Blossom...
                        </span>
                      ) : (
                        "Upload to Blossom Servers"
                      )}
                    </Button>
                  )}

                  {videoUrl && (
                    <div className="flex items-center gap-2 text-sm text-green-500">
                      <Check className="w-4 h-4" />
                      Uploaded — ready to publish
                    </div>
                  )}
                </div>
              )}

              {fileError && <p className="text-xs text-destructive mt-2">{fileError}</p>}
              <input ref={fileInputRef} type="file" accept="video/*" className="hidden" onChange={handleFileSelect} />
            </div>
          )}

          {/* URL paste section */}
          {mode === "url" && (
            <div>
              <label className="text-sm font-medium text-foreground mb-2 block">Video URL *</label>
              <Input value={videoUrl} onChange={(e) => setVideoUrl(e.target.value)} placeholder="https://... (.mp4, .webm, .m3u8)" className="bg-secondary" />
              <p className="text-xs text-muted-foreground mt-1">
                Upload to{" "}
                <a href="https://nostr.build" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">nostr.build</a>,{" "}
                <a href="https://void.cat" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">void.cat</a>, or any CDN first.
              </p>
            </div>
          )}

          {/* Show URL field if file was uploaded */}
          {mode === "file" && videoUrl && (
            <div>
              <label className="text-sm font-medium text-foreground mb-2 block">Video URL</label>
              <Input value={videoUrl} readOnly className="bg-secondary text-muted-foreground" />
            </div>
          )}

          <div>
            <label className="text-sm font-medium text-foreground mb-2 block">Title *</label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Give your video a title" className="bg-secondary" />
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
                <Loader2 className="w-4 h-4 animate-spin" />
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
