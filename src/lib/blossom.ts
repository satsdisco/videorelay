/**
 * Blossom upload — decentralized file hosting for Nostr.
 * Uploads to multiple Blossom servers for redundancy.
 */

export const BLOSSOM_SERVERS = [
  "https://blossom.primal.net",
  "https://blossom.band",
  "https://24242.io",
];

export interface UploadResult {
  server: string;
  url: string;
  success: boolean;
  error?: string;
}

export interface UploadProgress {
  server: string;
  status: "pending" | "uploading" | "success" | "error";
  error?: string;
}

/**
 * Calculate SHA-256 hash of a file.
 */
async function sha256Hash(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest("SHA-256", buffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
}

/**
 * Create a Blossom auth event (kind 24242) for upload authorization.
 */
async function createBlossomAuthEvent(hash: string, server: string): Promise<any> {
  if (!window.nostr) {
    throw new Error("No Nostr extension found");
  }

  const event = {
    kind: 24242,
    content: `Upload ${hash}`,
    tags: [
      ["t", "upload"],
      ["x", hash],
      ["expiration", String(Math.floor(Date.now() / 1000) + 300)], // 5 min
    ],
    created_at: Math.floor(Date.now() / 1000),
  };

  return (window.nostr as any).signEvent(event);
}

/**
 * Upload a file to a single Blossom server.
 */
async function uploadToServer(
  file: File,
  hash: string,
  server: string,
): Promise<UploadResult> {
  try {
    const authEvent = await createBlossomAuthEvent(hash, server);
    const authHeader = btoa(JSON.stringify(authEvent));

    const response = await fetch(`${server}/upload`, {
      method: "PUT",
      body: file,
      headers: {
        "Content-Type": file.type,
        Authorization: `Nostr ${authHeader}`,
      },
    });

    if (response.ok) {
      const result = await response.json();
      const url = result.url || `${server}/${hash}`;
      return { server, url, success: true };
    } else {
      const text = await response.text().catch(() => "");
      return {
        server,
        url: "",
        success: false,
        error: `HTTP ${response.status}${text ? `: ${text}` : ""}`,
      };
    }
  } catch (err: any) {
    return {
      server,
      url: "",
      success: false,
      error: err?.message || "Upload failed",
    };
  }
}

/**
 * Upload a file to all Blossom servers for redundancy.
 * Returns the first successful URL and status for each server.
 */
export async function uploadToBlossom(
  file: File,
  onProgress?: (progress: UploadProgress[]) => void,
): Promise<{ url: string; results: UploadResult[] }> {
  const hash = await sha256Hash(file);

  const progress: UploadProgress[] = BLOSSOM_SERVERS.map((server) => ({
    server,
    status: "pending",
  }));
  onProgress?.(progress);

  const results: UploadResult[] = [];
  let primaryUrl = "";

  // Upload to all servers in parallel
  const promises = BLOSSOM_SERVERS.map(async (server, i) => {
    progress[i].status = "uploading";
    onProgress?.([...progress]);

    const result = await uploadToServer(file, hash, server);
    results.push(result);

    if (result.success) {
      progress[i].status = "success";
      if (!primaryUrl) primaryUrl = result.url;
    } else {
      progress[i].status = "error";
      progress[i].error = result.error;
    }
    onProgress?.([...progress]);
  });

  await Promise.allSettled(promises);

  if (!primaryUrl) {
    throw new Error("Upload failed on all servers");
  }

  return { url: primaryUrl, results };
}

/**
 * Validate a video file before upload.
 */
export function validateVideoFile(file: File): string | null {
  const MAX_SIZE = 500 * 1024 * 1024; // 500MB
  const ALLOWED_TYPES = [
    "video/mp4",
    "video/webm",
    "video/quicktime",
    "video/x-matroska",
    "video/avi",
    "video/x-msvideo",
  ];

  if (!ALLOWED_TYPES.includes(file.type) && !file.type.startsWith("video/")) {
    return "Only video files are supported";
  }
  if (file.size > MAX_SIZE) {
    return `File too large (${(file.size / 1024 / 1024).toFixed(0)}MB). Maximum is 500MB.`;
  }
  return null;
}
