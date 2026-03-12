import {
  Home,
  TrendingUp,
  Zap,
  Users,
  Radio,
  Clock,
  ThumbsUp,
  ListVideo,
  Settings,
  Globe,
} from "lucide-react";

export type SidebarView = "home" | "trending" | "zapped" | "following" | "live";

interface SidebarProps {
  collapsed: boolean;
  activeView: SidebarView;
  onChangeView: (view: SidebarView) => void;
  onOpenRelays: () => void;
}

const mainLinks: { icon: typeof Home; label: string; view: SidebarView }[] = [
  { icon: Home, label: "Home", view: "home" },
  { icon: TrendingUp, label: "Trending", view: "trending" },
  { icon: Zap, label: "Most Zapped", view: "zapped" },
  { icon: Users, label: "Following", view: "following" },
  { icon: Radio, label: "Live", view: "live" },
];

const libraryLinks = [
  { icon: Clock, label: "Watch Later" },
  { icon: ThumbsUp, label: "Liked Videos" },
  { icon: ListVideo, label: "Playlists" },
];

const Sidebar = ({ collapsed, activeView, onChangeView, onOpenRelays }: SidebarProps) => {
  return (
    <aside
      className={`fixed left-0 top-14 bottom-0 z-40 bg-background border-r border-border transition-all duration-300 ${
        collapsed ? "w-[72px]" : "w-56"
      }`}
    >
      <nav className="flex flex-col h-full py-3 overflow-y-auto">
        {/* Main */}
        <div className="px-2 space-y-0.5">
          {mainLinks.map((link) => {
            const isActive = activeView === link.view;
            return (
              <button
                key={link.label}
                onClick={() => onChangeView(link.view)}
                className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-lg transition-colors ${
                  isActive
                    ? "bg-primary/10 text-primary"
                    : "text-secondary-foreground hover:bg-secondary"
                } ${collapsed ? "justify-center" : ""}`}
              >
                <link.icon
                  className={`w-5 h-5 shrink-0 ${isActive ? "text-primary" : ""}`}
                  fill={link.view === "zapped" && isActive ? "currentColor" : "none"}
                />
                {!collapsed && (
                  <span className="text-sm font-medium truncate">{link.label}</span>
                )}
                {link.view === "live" && !collapsed && (
                  <span className="ml-auto w-2 h-2 bg-red-500 rounded-full animate-pulse" />
                )}
              </button>
            );
          })}
        </div>

        <div className="mx-4 my-3 border-t border-border" />

        {/* Library */}
        {!collapsed && (
          <div className="px-4 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Library
            </span>
          </div>
        )}
        <div className="px-2 space-y-0.5">
          {libraryLinks.map((link) => (
            <button
              key={link.label}
              className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-secondary-foreground hover:bg-secondary transition-colors ${
                collapsed ? "justify-center" : ""
              }`}
            >
              <link.icon className="w-5 h-5 shrink-0" />
              {!collapsed && (
                <span className="text-sm font-medium truncate">{link.label}</span>
              )}
            </button>
          ))}
        </div>

        <div className="mx-4 my-3 border-t border-border" />

        {/* Settings */}
        <div className="px-2 space-y-0.5">
          <button
            onClick={onOpenRelays}
            className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-secondary-foreground hover:bg-secondary transition-colors ${
              collapsed ? "justify-center" : ""
            }`}
          >
            <Globe className="w-5 h-5 shrink-0" />
            {!collapsed && (
              <span className="text-sm font-medium truncate">Relays</span>
            )}
          </button>
          <button
            className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-secondary-foreground hover:bg-secondary transition-colors ${
              collapsed ? "justify-center" : ""
            }`}
          >
            <Settings className="w-5 h-5 shrink-0" />
            {!collapsed && (
              <span className="text-sm font-medium truncate">Settings</span>
            )}
          </button>
        </div>

        {/* Footer */}
        {!collapsed && (
          <div className="mt-auto px-4 py-4">
            <p className="text-xs text-muted-foreground leading-relaxed">
              Built on Nostr protocol.
              <br />
              Censorship-resistant. Zap-powered.
            </p>
          </div>
        )}
      </nav>
    </aside>
  );
};

export default Sidebar;
