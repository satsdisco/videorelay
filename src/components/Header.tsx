import { useState, useRef, useEffect } from "react";
import { Search, Upload, Bell, Menu, LogIn, LogOut, User, Copy, Check } from "lucide-react";
import { useNostrAuth } from "@/hooks/useNostrAuth";
import logo from "@/assets/logo.png";

interface HeaderProps {
  onToggleSidebar: () => void;
  onSearch?: (query: string) => void;
}

const Header = ({ onToggleSidebar }: HeaderProps) => {
  const { isLoggedIn, pubkey, profile, isExtensionAvailable, login, logout } = useNostrAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const handleLogin = async () => {
    try {
      await login();
    } catch (err: any) {
      alert(err.message || "Login failed");
    }
  };

  const copyPubkey = () => {
    if (pubkey) {
      navigator.clipboard.writeText(pubkey);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const displayName = profile?.displayName || profile?.name || pubkey?.slice(0, 8);
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

            {/* User dropdown */}
            <div className="relative ml-2" ref={dropdownRef}>
              <button
                onClick={() => setDropdownOpen(!dropdownOpen)}
                className="w-8 h-8 rounded-full overflow-hidden bg-gradient-to-br from-primary to-accent flex items-center justify-center text-xs font-bold text-primary-foreground ring-2 ring-transparent hover:ring-primary/50 transition-all"
                title={`Logged in as ${displayName}`}
              >
                {avatar ? (
                  <img src={avatar} alt="" className="w-full h-full object-cover" />
                ) : (
                  displayName?.[0]?.toUpperCase() || "?"
                )}
              </button>

              {dropdownOpen && (
                <div className="absolute right-0 top-full mt-2 w-72 bg-background border border-border rounded-xl shadow-xl overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 z-50">
                  {/* Profile header */}
                  <div className="p-4 border-b border-border bg-secondary/30">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-br from-primary to-accent flex items-center justify-center text-sm font-bold text-primary-foreground shrink-0">
                        {avatar ? (
                          <img src={avatar} alt="" className="w-full h-full object-cover" />
                        ) : (
                          displayName?.[0]?.toUpperCase() || "?"
                        )}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-foreground truncate">{displayName}</p>
                        {profile?.nip05 && (
                          <p className="text-xs text-primary truncate">{profile.nip05}</p>
                        )}
                        <button
                          onClick={copyPubkey}
                          className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors mt-0.5"
                        >
                          {copied ? <Check className="w-3 h-3 text-green-500" /> : <Copy className="w-3 h-3" />}
                          {copied ? "Copied!" : `${pubkey?.slice(0, 12)}...${pubkey?.slice(-4)}`}
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Menu items */}
                  <div className="p-1">
                    <button
                      onClick={() => setDropdownOpen(false)}
                      className="flex items-center gap-3 w-full px-3 py-2.5 text-sm text-foreground hover:bg-secondary rounded-lg transition-colors"
                    >
                      <User className="w-4 h-4 text-muted-foreground" />
                      Your Channel
                    </button>
                    <button
                      onClick={() => {
                        logout();
                        setDropdownOpen(false);
                      }}
                      className="flex items-center gap-3 w-full px-3 py-2.5 text-sm text-destructive hover:bg-destructive/10 rounded-lg transition-colors"
                    >
                      <LogOut className="w-4 h-4" />
                      Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
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
