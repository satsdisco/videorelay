import { useState, useEffect, lazy, Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, useNavigate, useLocation } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { NostrAuthProvider } from "@/hooks/useNostrAuth";
import MobileNav from "@/components/MobileNav";
import MobileSearch from "@/components/MobileSearch";
import { type SidebarView } from "@/components/Sidebar";
import { Loader2 } from "lucide-react";
import ErrorBoundary from "@/components/ErrorBoundary";

// Eager: homepage is always needed
import Index from "./pages/Index.tsx";

// Lazy: loaded on demand
const Watch = lazy(() => import("./pages/Watch.tsx"));
const Channel = lazy(() => import("./pages/Channel.tsx"));
const Upload = lazy(() => import("./pages/Upload.tsx"));
const Shorts = lazy(() => import("./pages/Shorts.tsx"));
const Live = lazy(() => import("./pages/Live.tsx"));
const Settings = lazy(() => import("./pages/Settings.tsx"));
const NotFound = lazy(() => import("./pages/NotFound.tsx"));

const queryClient = new QueryClient();

const PageLoader = () => (
  <div className="flex items-center justify-center min-h-[50vh]">
    <Loader2 className="w-6 h-6 text-primary animate-spin" />
  </div>
);

// Map URL paths to sidebar views
const VIEW_ROUTES: Record<string, SidebarView> = {
  "/": "home",
  "/trending": "trending",
  "/zapped": "zapped",
  "/most-zapped": "zapped",
  "/following": "following",
};

const VIEW_PATHS: Record<SidebarView, string> = {
  home: "/",
  trending: "/trending",
  zapped: "/zapped",
  following: "/following",
  live: "/live",
};

const AppRoutes = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [activeView, setActiveView] = useState<SidebarView>(() => {
    return VIEW_ROUTES[window.location.pathname] || "home";
  });
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  // Sync activeView when URL changes (back/forward navigation)
  useEffect(() => {
    const view = VIEW_ROUTES[location.pathname];
    if (view && view !== activeView) {
      setActiveView(view);
    }
  }, [location.pathname]);

  const handleViewChange = (view: SidebarView) => {
    setActiveView(view);
    navigate(VIEW_PATHS[view] || "/");
  };

  return (
    <>
      <ErrorBoundary>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          {["/", "/trending", "/zapped", "/most-zapped", "/following"].map((path) => (
            <Route
              key={path}
              path={path}
              element={
                <Index
                  activeView={activeView}
                  setActiveView={setActiveView}
                  mobileSearchOpen={mobileSearchOpen}
                  setMobileSearchOpen={setMobileSearchOpen}
                  searchQuery={searchQuery}
                  setSearchQuery={setSearchQuery}
                />
              }
            />
          ))}
          <Route path="/watch/:id" element={<Watch />} />
          <Route path="/shorts" element={<Shorts />} />
          <Route path="/live" element={<Live />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/channel/:pubkey" element={<Channel />} />
          <Route path="/upload" element={<Upload />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
      </ErrorBoundary>

      <MobileNav
        activeView={activeView}
        onChangeView={handleViewChange}
        onSearchOpen={() => {
          setMobileSearchOpen(true);
          navigate("/");
        }}
      />

      <MobileSearch
        open={mobileSearchOpen}
        onClose={() => setMobileSearchOpen(false)}
        onSearch={setSearchQuery}
      />
    </>
  );
};

const App = () => (
  <NostrAuthProvider>
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </TooltipProvider>
    </QueryClientProvider>
  </NostrAuthProvider>
);

export default App;
