import { useState, useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

const categories = [
  "All",
  "Bitcoin",
  "Lightning",
  "Nostr",
  "Privacy",
  "Podcast",
  "Documentary",
  "Education",
  "Tutorial",
  "Entertainment",
  "Music",
  "Gaming",
  "News",
  "Mining",
  "Dev",
  "Security",
  "Finance",
  "Philosophy",
  "Art",
  "Comedy",
];

interface CategoryBarProps {
  onCategoryChange?: (category: string) => void;
}

const CategoryBar = ({ onCategoryChange }: CategoryBarProps) => {
  const [active, setActive] = useState("All");
  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);

  const checkScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    setCanScrollLeft(el.scrollLeft > 0);
    setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 10);
  };

  const scroll = (dir: "left" | "right") => {
    scrollRef.current?.scrollBy({ left: dir === "left" ? -200 : 200, behavior: "smooth" });
  };

  const handleClick = (cat: string) => {
    setActive(cat);
    onCategoryChange?.(cat);
  };

  return (
    <div className="relative flex items-center">
      {/* Left fade + arrow */}
      {canScrollLeft && (
        <>
          <div className="absolute left-0 z-10 w-16 h-full bg-gradient-to-r from-background to-transparent pointer-events-none" />
          <button
            onClick={() => scroll("left")}
            className="absolute left-1 z-20 p-1.5 rounded-full bg-background/95 border border-border shadow-md hover:bg-secondary transition-colors"
          >
            <ChevronLeft className="w-4 h-4 text-foreground" />
          </button>
        </>
      )}
      <div
        ref={scrollRef}
        onScroll={checkScroll}
        className="flex items-center gap-2 overflow-x-auto py-3 px-1 scrollbar-hide scroll-smooth"
      >
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => handleClick(cat)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-all ${
              active === cat
                ? "bg-primary text-primary-foreground"
                : "bg-secondary text-secondary-foreground hover:bg-muted"
            }`}
          >
            {cat}
          </button>
        ))}
      </div>
      {/* Right fade + arrow */}
      {canScrollRight && (
        <>
          <div className="absolute right-0 z-10 w-16 h-full bg-gradient-to-l from-background to-transparent pointer-events-none" />
          <button
            onClick={() => scroll("right")}
            className="absolute right-1 z-20 p-1.5 rounded-full bg-background/95 border border-border shadow-md hover:bg-secondary transition-colors"
          >
            <ChevronRight className="w-4 h-4 text-foreground" />
          </button>
        </>
      )}
    </div>
  );
};

export default CategoryBar;
