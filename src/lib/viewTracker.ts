/**
 * Local view tracking — tracks which videos you've watched.
 * Stored in localStorage, no network calls.
 */
import { safeSetItem, safeGetItem } from "./safeStorage";

const STORAGE_KEY = "videorelay_views";

interface ViewRecord {
  [videoId: string]: number; // timestamp of first view
}

function loadViews(): ViewRecord {
  try {
    return JSON.parse(safeGetItem(STORAGE_KEY) || "{}");
  } catch {
    return {};
  }
}

function saveViews(views: ViewRecord) {
  try {
    safeSetItem(STORAGE_KEY, JSON.stringify(views));
  } catch {}
}

/**
 * Record a view for a video.
 */
export function recordView(videoId: string) {
  const views = loadViews();
  if (!views[videoId]) {
    views[videoId] = Date.now();
    saveViews(views);
  }
}

/**
 * Check if a video has been watched.
 */
export function hasWatched(videoId: string): boolean {
  const views = loadViews();
  return !!views[videoId];
}

/**
 * Get total number of unique videos watched.
 */
export function getTotalViews(): number {
  return Object.keys(loadViews()).length;
}

/**
 * Get all watched video IDs.
 */
export function getWatchedIds(): Set<string> {
  return new Set(Object.keys(loadViews()));
}
