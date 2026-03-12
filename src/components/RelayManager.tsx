import { useState } from "react";
import { X, Plus, RotateCcw, Globe, Wifi, WifiOff } from "lucide-react";
import { useRelayStore, DEFAULT_RELAYS } from "@/hooks/useRelayStore";

interface RelayManagerProps {
  open: boolean;
  onClose: () => void;
}

const RelayManager = ({ open, onClose }: RelayManagerProps) => {
  const { relays, addRelay, removeRelay, toggleRelay, resetToDefaults } = useRelayStore();
  const [newRelay, setNewRelay] = useState("");
  const [error, setError] = useState("");

  if (!open) return null;

  const handleAdd = () => {
    setError("");
    const url = newRelay.trim();
    if (!url) return;
    if (!url.startsWith("wss://") && !url.startsWith("ws://")) {
      setError("Relay URL must start with wss:// or ws://");
      return;
    }
    if (relays.some((r) => r.url === url.replace(/\/+$/, ""))) {
      setError("Relay already added");
      return;
    }
    addRelay(url);
    setNewRelay("");
  };

  const enabledCount = relays.filter((r) => r.enabled).length;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-lg mx-4 bg-background border border-border rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border">
          <div className="flex items-center gap-2">
            <Globe className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-bold text-foreground">Relay Settings</h2>
            <span className="text-xs text-muted-foreground bg-secondary px-2 py-0.5 rounded-full">
              {enabledCount} active
            </span>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-secondary transition-colors">
            <X className="w-5 h-5 text-muted-foreground" />
          </button>
        </div>

        {/* Add relay */}
        <div className="p-4 border-b border-border">
          <div className="flex gap-2">
            <input
              type="text"
              value={newRelay}
              onChange={(e) => { setNewRelay(e.target.value); setError(""); }}
              onKeyDown={(e) => e.key === "Enter" && handleAdd()}
              placeholder="wss://relay.example.com"
              className="flex-1 bg-secondary border border-border rounded-lg px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground outline-none focus:border-primary/50 transition-colors"
            />
            <button
              onClick={handleAdd}
              className="flex items-center gap-1 px-3 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:opacity-90 transition-opacity"
            >
              <Plus className="w-4 h-4" />
              Add
            </button>
          </div>
          {error && <p className="text-xs text-destructive mt-1.5">{error}</p>}
        </div>

        {/* Relay list */}
        <div className="max-h-80 overflow-y-auto p-2">
          {relays.map((relay) => {
            const isDefault = DEFAULT_RELAYS.includes(relay.url);
            return (
              <div
                key={relay.url}
                className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-secondary/50 transition-colors group"
              >
                <button
                  onClick={() => toggleRelay(relay.url)}
                  className={`shrink-0 transition-colors ${relay.enabled ? "text-green-500" : "text-muted-foreground"}`}
                  title={relay.enabled ? "Disable relay" : "Enable relay"}
                >
                  {relay.enabled ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
                </button>
                <div className="flex-1 min-w-0">
                  <p className={`text-sm truncate ${relay.enabled ? "text-foreground" : "text-muted-foreground line-through"}`}>
                    {relay.url}
                  </p>
                </div>
                {!isDefault && (
                  <button
                    onClick={() => removeRelay(relay.url)}
                    className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-all"
                    title="Remove relay"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between p-4 border-t border-border bg-secondary/20">
          <button
            onClick={resetToDefaults}
            className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Reset to defaults
          </button>
          <p className="text-xs text-muted-foreground">
            Changes apply on next refresh
          </p>
        </div>
      </div>
    </div>
  );
};

export default RelayManager;
