import { Component, type ReactNode } from "react";
import { RefreshCw } from "lucide-react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("ErrorBoundary caught:", error, errorInfo);

    // Auto-reload on stale chunk errors (happens after deploys)
    if (error.message?.includes("dynamically imported module") ||
        error.message?.includes("Failed to fetch") ||
        error.message?.includes("Loading chunk")) {
      // Only auto-reload once to avoid infinite loops
      const key = "videorelay_chunk_reload";
      const last = localStorage.getItem(key);
      const now = Date.now();
      if (!last || now - parseInt(last) > 30000) {
        localStorage.setItem(key, now.toString());
        window.location.reload();
        return;
      }
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    // Force full reload to get fresh chunks
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <div className="flex flex-col items-center justify-center min-h-[40vh] px-4">
          <p className="text-foreground font-medium mb-2">Something broke.</p>
          <p className="text-muted-foreground text-sm mb-4 text-center max-w-md">
            {this.state.error?.message || "An unexpected error occurred."}
          </p>
          <button
            onClick={this.handleReset}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
          >
            <RefreshCw className="w-4 h-4" />
            Try Again
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
