import { useState, useRef, useEffect } from "react";
import { Search, ArrowLeft, X } from "lucide-react";

interface MobileSearchProps {
  open: boolean;
  onClose: () => void;
  onSearch: (query: string) => void;
}

const MobileSearch = ({ open, onClose, onSearch }: MobileSearchProps) => {
  const [query, setQuery] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (open) {
      setTimeout(() => inputRef.current?.focus(), 100);
    } else {
      setQuery("");
    }
  }, [open]);

  const handleChange = (val: string) => {
    setQuery(val);
    onSearch(val);
  };

  const handleClear = () => {
    setQuery("");
    onSearch("");
    inputRef.current?.focus();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[60] bg-background md:hidden">
      <div className="flex items-center gap-2 h-14 px-3 border-b border-border">
        <button onClick={onClose} className="p-2 rounded-lg hover:bg-secondary transition-colors">
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </button>
        <div className="flex-1 flex items-center bg-secondary rounded-full overflow-hidden border border-border focus-within:border-primary/50 transition-colors">
          <Search className="w-4 h-4 text-muted-foreground ml-3 shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => handleChange(e.target.value)}
            placeholder="Search videos, creators, tags..."
            className="flex-1 bg-transparent px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none"
          />
          {query && (
            <button onClick={handleClear} className="p-2">
              <X className="w-4 h-4 text-muted-foreground" />
            </button>
          )}
        </div>
      </div>
      {!query && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
          <Search className="w-8 h-8 mb-3 opacity-40" />
          <p className="text-sm">Search for videos across Nostr relays</p>
        </div>
      )}
    </div>
  );
};

export default MobileSearch;
