import { useState } from "react";

const categories = [
  "All",
  "Bitcoin",
  "Lightning",
  "Nostr",
  "Privacy",
  "Mining",
  "Tutorial",
  "Podcast",
  "Music",
  "Gaming",
  "News",
];

interface CategoryBarProps {
  onCategoryChange?: (category: string) => void;
}

const CategoryBar = ({ onCategoryChange }: CategoryBarProps) => {
  const [active, setActive] = useState("All");

  const handleClick = (cat: string) => {
    setActive(cat);
    onCategoryChange?.(cat);
  };

  return (
    <div className="flex items-center gap-2 overflow-x-auto py-3 px-1 scrollbar-hide">
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
  );
};

export default CategoryBar;
