import { createRoot } from "react-dom/client";
import "@fontsource/space-grotesk/400.css";
import "@fontsource/space-grotesk/500.css";
import "@fontsource/space-grotesk/600.css";
import "@fontsource/space-grotesk/700.css";
import App from "./App.tsx";
import "./index.css";

// Apply saved theme before first render to prevent flash
const savedTheme = localStorage.getItem("videorelay_theme") || "dark";
const resolved = savedTheme === "system"
  ? (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
  : savedTheme;
document.documentElement.classList.add(resolved);

createRoot(document.getElementById("root")!).render(<App />);
