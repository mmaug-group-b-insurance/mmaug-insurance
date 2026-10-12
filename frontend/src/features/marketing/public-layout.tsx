import { type ReactNode, useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import { Menu, X, Moon, Sun, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTrigger, SheetClose, SheetTitle } from "@/components/ui/sheet";
import { useTheme } from "@/components/theme-provider";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/auth/use-auth";

const navItems = [
  { to: "/features", label: "Features" },
  { to: "/how-it-works", label: "How It Works" },
  { to: "/security", label: "Security" },
  { to: "/responsible-ai", label: "Responsible AI" },
  { to: "/faq", label: "FAQ" },
];

export function PublicLayout({ children }: { children: ReactNode }) {
  const { resolved, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const { isAuthenticated } = useAuth();
const portalTo = isAuthenticated ? "/app" : "/login";

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
        <div className="container-page flex h-16 items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-xs font-bold text-primary-foreground">
              M
            </div>
            <span className="hidden text-sm font-semibold tracking-tight sm:inline">MMAUG Claims Agent</span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex" aria-label="Primary">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    "rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground",
                    isActive ? "text-foreground" : "text-muted-foreground"
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setTheme(resolved === "dark" ? "light" : "dark")}
              aria-label="Toggle theme"
            >
              {resolved === "dark" ? <Sun /> : <Moon />}
            </Button>
            <Link to={portalTo} className="hidden sm:inline-flex">
  <Button variant="ghost" size="sm">{isAuthenticated ? "Open app" : "Sign in"}</Button>
</Link>
            <Link to={portalTo}>
  <Button size="sm">
    {isAuthenticated ? "Open Portal" : "Access Portal"} <ArrowRight />
  </Button>
</Link>
            <Sheet open={open} onOpenChange={setOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu">
                  <Menu />
                </Button>
              </SheetTrigger>
              <SheetContent side="right" className="w-72">
                <SheetTitle className="sr-only">Navigation</SheetTitle>
                <nav className="mt-6 flex flex-col gap-1" aria-label="Mobile">
                  {navItems.map((item) => (
                    <SheetClose asChild key={item.to}>
                      <NavLink
                        to={item.to}
                        className={({ isActive }) =>
                          cn(
                            "rounded-md px-3 py-2 text-sm font-medium",
                            isActive ? "bg-accent text-accent-foreground" : "text-muted-foreground"
                          )
                        }
                      >
                        {item.label}
                      </NavLink>
                    </SheetClose>
                  ))}
                 <SheetClose asChild>
  <NavLink to={portalTo} className="rounded-md px-3 py-2 text-sm font-medium text-muted-foreground">
    {isAuthenticated ? "Open app" : "Sign in"}
  </NavLink>
</SheetClose>
                </nav>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </header>

      <main className="flex-1">{children}</main>

      <footer className="border-t bg-muted/30">
        <div className="container-page grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-xs font-bold text-primary-foreground">M</div>
              <span className="text-sm font-semibold">MMAUG Claims Agent</span>
            </div>
            <p className="mt-3 text-xs text-muted-foreground">
              AI-assisted claims processing with evidence traceability and human oversight.
              Decision-support only — never autonomous.
            </p>
          </div>
          <FooterCol title="Product">
            <FooterLink to="/features">Features</FooterLink>
            <FooterLink to="/how-it-works">How It Works</FooterLink>
            <FooterLink to="/security">Security</FooterLink>
            <FooterLink to="/responsible-ai">Responsible AI</FooterLink>
          </FooterCol>
          <FooterCol title="Access">
            <FooterLink to="/login">Sign in</FooterLink>
            <FooterLink to="/faq">FAQ</FooterLink>
          </FooterCol>
          <FooterCol title="Legal">
            <span className="text-xs text-muted-foreground">Privacy and terms pages are not yet available.</span>
          </FooterCol>
        </div>
        <div className="border-t">
          <div className="container-page flex flex-col gap-2 py-4 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
            <p>© {new Date().getFullYear()} MMAUG Insurance Ltd (training simulation). All rights reserved.</p>
            <p className="font-mono">Environment: {import.meta.env.VITE_ENV_NAME ?? "Local"} · v{import.meta.env.VITE_APP_VERSION ?? "0.0.0"}</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

function FooterCol({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <h3 className="text-xs font-semibold uppercase tracking-wide text-foreground">{title}</h3>
      <ul className="mt-3 space-y-2">{children}</ul>
    </div>
  );
}

function FooterLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <li>
      <Link to={to} className="text-xs text-muted-foreground transition-colors hover:text-foreground">
        {children}
      </Link>
    </li>
  );
}