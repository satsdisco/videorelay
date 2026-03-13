import { useState, lazy, Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, useNavigate } from "react-router-dom";
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
const Settings = lazy(() => import("./pages/Settings.tsx"));
const NotFound = lazy(() => import("./pages/NotFound.tsx"));

const queryClient = new QueryClient();

const PageLoader = () => (
  <div className="flex items-center justify-center min-h-[50vh]">
    <Loader2 className="w-6 h-6 text-primary animate-spin" />
  </div>
);

const AppRoutes = () => {
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState<SidebarView>("home");
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const handleViewChange = (view: SidebarView) => {
    setActiveView(view);
    navigate("/");
  };

  return (
    <>
      <ErrorBoundary>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route
            path="/"
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
          <Route path="/watch/:id" element={<Watch />} />
          <Route path="/shorts" element={<Shorts />} />
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
