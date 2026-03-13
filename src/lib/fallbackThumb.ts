/**
 * Generate a deterministic gradient SVG data URL as fallback thumbnail.
 * Uses the event ID to pick consistent colors — no image files needed.
 */

const GRADIENTS = [
  ["#6366f1", "#8b5cf6"], // indigo → violet
  ["#ec4899", "#f43f5e"], // pink → rose
  ["#f59e0b", "#ef4444"], // amber → red
  ["#10b981", "#06b6d4"], // emerald → cyan
  ["#3b82f6", "#6366f1"], // blue → indigo
  ["#8b5cf6", "#ec4899"], // violet → pink
  ["#14b8a6", "#22d3ee"], // teal → cyan
  ["#f97316", "#eab308"], // orange → yellow
];

export function getFallbackThumb(id: string): string {
  const index = id.charCodeAt(0) % GRADIENTS.length;
  const [from, to] = GRADIENTS[index];

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
    <defs>
      <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${from}" />
        <stop offset="100%" stop-color="${to}" />
      </linearGradient>
    </defs>
    <rect width="640" height="360" fill="url(#g)" />
    <text x="320" y="180" text-anchor="middle" dominant-baseline="central" fill="white" opacity="0.3" font-family="system-ui" font-size="48" font-weight="bold">▶</text>
  </svg>`;

  return `data:image/svg+xml,${encodeURIComponent(svg)}`;
}
