import { Home, TrendingUp, Zap, Users, Search } from "lucide-react";
import type { SidebarView } from "./Sidebar";

interface MobileNavProps {
  activeView: SidebarView;
  onChangeView: (view: SidebarView) => void;
  onSearchOpen: () => void;
}

const navItems: { icon: typeof Home; label: string; view: SidebarView }[] = [
  { icon: Home, label: "Home", view: "home" },
  { icon: TrendingUp, label: "Trending", view: "trending" },
  { icon: Zap, label: "Zapped", view: "zapped" },
  { icon: Users, label: "Following", view: "following" },
];

const MobileNav = ({ activeView, onChangeView, onSearchOpen }: MobileNavProps) => {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 md:hidden bg-background/95 backdrop-blur-md border-t border-border safe-area-bottom">
      <div className="flex items-center justify-around h-14">
        {navItems.map((item) => {
          const isActive = activeView === item.view;
          return (
            <button
              key={item.view}
              onClick={() => onChangeView(item.view)}
              className="flex flex-col items-center justify-center gap-0.5 flex-1 h-full transition-colors"
            >
              <item.icon
                className={`w-5 h-5 ${isActive ? "text-primary" : "text-muted-foreground"}`}
                fill={item.view === "zapped" && isActive ? "currentColor" : "none"}
              />
              <span
                className={`text-[10px] font-medium ${
                  isActive ? "text-primary" : "text-muted-foreground"
                }`}
              >
                {item.label}
              </span>
            </button>
          );
        })}
        <button
          onClick={onSearchOpen}
          className="flex flex-col items-center justify-center gap-0.5 flex-1 h-full transition-colors"
        >
          <Search className="w-5 h-5 text-muted-foreground" />
          <span className="text-[10px] font-medium text-muted-foreground">Search</span>
        </button>
      </div>
    </nav>
  );
};

export default MobileNav;
