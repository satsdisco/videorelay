import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, useNavigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { NostrAuthProvider } from "@/hooks/useNostrAuth";
import MobileNav from "@/components/MobileNav";
import MobileSearch from "@/components/MobileSearch";
import { type SidebarView } from "@/components/Sidebar";
import Index from "./pages/Index.tsx";
import Watch from "./pages/Watch.tsx";
import Channel from "./pages/Channel.tsx";
import Upload from "./pages/Upload.tsx";
import NotFound from "./pages/NotFound.tsx";

const queryClient = new QueryClient();

const AppRoutes = () => {
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState<SidebarView>("home");
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const handleViewChange = (view: SidebarView) => {
    setActiveView(view);
    // Always navigate home when switching views from bottom nav
    navigate("/");
  };

  return (
    <>
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
        <Route path="/channel/:pubkey" element={<Channel />} />
        <Route path="/upload" element={<Upload />} />
        <Route path="*" element={<NotFound />} />
      </Routes>

      {/* Persistent mobile bottom nav */}
      <MobileNav
        activeView={activeView}
        onChangeView={handleViewChange}
        onSearchOpen={() => {
          setMobileSearchOpen(true);
          navigate("/");
        }}
      />

      {/* Mobile search overlay */}
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
