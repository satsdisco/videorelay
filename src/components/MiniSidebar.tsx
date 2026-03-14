import { useNavigate, useLocation } from "react-router-dom";
import {
  Home,
  TrendingUp,
  Zap,
  Users,
  Radio,
  Film,
  Settings,
} from "lucide-react";

const links = [
  { icon: Home, label: "Home", path: "/" },
  { icon: TrendingUp, label: "Trending", path: "/trending" },
  { icon: Zap, label: "Most Zapped", path: "/zapped" },
  { icon: Users, label: "Following", path: "/following" },
  { icon: Radio, label: "Live", path: "/live" },
  { icon: Film, label: "Shorts", path: "/shorts" },
  { icon: Settings, label: "Settings", path: "/settings" },
];

/**
 * Collapsed icon-only sidebar for non-Index pages (Watch, Channel, etc.)
 * Hidden on mobile (MobileNav handles that).
 */
const MiniSidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <aside className="hidden md:flex fixed left-0 top-0 bottom-0 z-40 w-[72px] bg-background border-r border-border flex-col py-16">
      <nav className="flex flex-col items-center gap-1 px-2">
        {links.map((link) => {
          const isActive = location.pathname === link.path;
          return (
            <button
              key={link.path}
              onClick={() => navigate(link.path)}
              title={link.label}
              className={`flex items-center justify-center w-12 h-12 rounded-lg transition-colors ${
                isActive
                  ? "bg-primary/10 text-primary"
                  : "text-secondary-foreground hover:bg-secondary"
              }`}
            >
              <link.icon
                className={`w-5 h-5 ${isActive ? "text-primary" : ""}`}
                fill={link.path === "/zapped" && isActive ? "currentColor" : "none"}
              />
            </button>
          );
        })}
      </nav>
    </aside>
  );
};

export default MiniSidebar;
