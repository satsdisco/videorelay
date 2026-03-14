import { useNavigate, useLocation } from "react-router-dom";
import {
  Home,
  TrendingUp,
  Zap,
  Users,
  Radio,
  Film,
  Settings,
  Upload,
} from "lucide-react";

const links = [
  { icon: Home, label: "Home", path: "/" },
  { icon: TrendingUp, label: "Trending", path: "/trending" },
  { icon: Zap, label: "Most Zapped", path: "/zapped" },
  { icon: Users, label: "Following", path: "/following" },
  { icon: Radio, label: "Live", path: "/live" },
  { icon: Film, label: "Shorts", path: "/shorts" },
  { icon: Upload, label: "Upload", path: "/upload" },
  { icon: Settings, label: "Settings", path: "/settings" },
];

interface MiniSidebarProps {
  collapsed?: boolean;
}

const MiniSidebar = ({ collapsed = true }: MiniSidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <aside
      className={`hidden md:flex fixed left-0 top-0 bottom-0 z-40 bg-background border-r border-border flex-col py-16 transition-all duration-300 ${
        collapsed ? "w-[72px]" : "w-52"
      }`}
    >
      <nav className={`flex flex-col gap-0.5 ${collapsed ? "items-center px-2" : "px-2"}`}>
        {links.map((link) => {
          const isActive = location.pathname === link.path;
          return (
            <button
              key={link.path}
              onClick={() => navigate(link.path)}
              title={collapsed ? link.label : undefined}
              className={`flex items-center gap-3 rounded-lg transition-colors ${
                collapsed ? "justify-center w-12 h-12" : "px-3 py-2.5 w-full"
              } ${
                isActive
                  ? "bg-primary/10 text-primary"
                  : "text-secondary-foreground hover:bg-secondary"
              }`}
            >
              <link.icon
                className={`w-5 h-5 shrink-0 ${isActive ? "text-primary" : ""}`}
                fill={link.path === "/zapped" && isActive ? "currentColor" : "none"}
              />
              {!collapsed && (
                <span className="text-sm font-medium truncate">{link.label}</span>
              )}
            </button>
          );
        })}
      </nav>

      {!collapsed && (
        <div className="mt-auto px-4 py-4">
          <p className="text-xs text-muted-foreground leading-relaxed">
            Built on Nostr protocol.
            <br />
            Censorship-resistant. Zap-powered.
          </p>
        </div>
      )}
    </aside>
  );
};

export default MiniSidebar;
