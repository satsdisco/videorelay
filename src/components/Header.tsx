import { Search, Upload, Bell, Zap, Menu, LogIn } from "lucide-react";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import logo from "@/assets/logo.png";

interface HeaderProps {
  onToggleSidebar: () => void;
}

const Header = ({ onToggleSidebar }: HeaderProps) => {
  const { isLoggedIn, profile, isExtensionAvailable, login, logout } = useNostrAuth();

  const handleLogin = async () => {
    try {
      await login();
    } catch (err: any) {
      alert(err.message || "Login failed");
    }
  };

  const displayName = profile?.displayName || profile?.name || profile?.pubkey?.slice(0, 8);
  const avatar = profile?.picture;

  return (
    <header className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between h-14 px-4 bg-background/95 backdrop-blur-sm border-b border-border">
      {/* Left */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="p-2 rounded-lg hover:bg-secondary transition-colors"
        >
          <Menu className="w-5 h-5 text-foreground" />
        </button>
        <div className="flex items-center gap-2">
          <img src={logo} alt="NostrTube" className="w-7 h-7" />
          <span className="text-lg font-bold tracking-tight text-foreground">
            Nostr<span className="text-primary">Tube</span>
          </span>
        </div>
      </div>

      {/* Center - Search */}
      <div className="hidden sm:flex items-center flex-1 max-w-xl mx-8">
        <div className="flex items-center w-full bg-secondary rounded-full overflow-hidden border border-border focus-within:border-primary/50 transition-colors">
          <input
            type="text"
            placeholder="Search videos, creators, relays..."
            className="flex-1 bg-transparent px-4 py-2 text-sm text-foreground placeholder:text-muted-foreground outline-none"
          />
          <button className="px-4 py-2 bg-secondary hover:bg-muted transition-colors border-l border-border">
            <Search className="w-4 h-4 text-muted-foreground" />
          </button>
        </div>
      </div>

      {/* Right */}
      <div className="flex items-center gap-1">
        <button className="sm:hidden p-2 rounded-lg hover:bg-secondary transition-colors">
          <Search className="w-5 h-5 text-foreground" />
        </button>

        {isLoggedIn ? (
          <>
            <button className="p-2 rounded-lg hover:bg-secondary transition-colors group">
              <Upload className="w-5 h-5 text-foreground group-hover:text-primary transition-colors" />
            </button>
            <button className="p-2 rounded-lg hover:bg-secondary transition-colors relative">
              <Bell className="w-5 h-5 text-foreground" />
            </button>
            <button
              onClick={logout}
              className="ml-2 w-8 h-8 rounded-full overflow-hidden bg-gradient-to-br from-primary to-zap flex items-center justify-center text-xs font-bold text-primary-foreground"
              title={`Logged in as ${displayName}`}
            >
              {avatar ? (
                <img src={avatar} alt="" className="w-full h-full object-cover" />
              ) : (
                displayName?.[0]?.toUpperCase() || "?"
              )}
            </button>
          </>
        ) : (
          <button
            onClick={handleLogin}
            className="flex items-center gap-2 ml-2 px-4 py-2 rounded-full bg-primary text-primary-foreground hover:opacity-90 transition-opacity text-sm font-medium"
          >
            <LogIn className="w-4 h-4" />
            {isExtensionAvailable ? "Sign in with Nostr" : "Install Nostr Extension"}
          </button>
        )}
      </div>
    </header>
  );
};

export default Header;
