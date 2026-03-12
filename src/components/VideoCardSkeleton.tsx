const VideoCardSkeleton = () => (
  <div className="animate-pulse">
    <div className="aspect-video rounded-xl bg-secondary mb-3" />
    <div className="flex gap-3">
      <div className="w-9 h-9 rounded-full bg-secondary shrink-0 mt-0.5" />
      <div className="flex-1 space-y-2">
        <div className="h-4 bg-secondary rounded w-full" />
        <div className="h-3 bg-secondary rounded w-3/4" />
        <div className="h-3 bg-secondary rounded w-1/2" />
      </div>
    </div>
  </div>
);

export default VideoCardSkeleton;
