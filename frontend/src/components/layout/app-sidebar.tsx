import { useState } from "react";
import { NavLink, Link, useLocation } from "react-router-dom";
import {
  LayoutDashboard, FileText, PlusCircle, ScrollText, Info, ChevronLeft, ChevronRight, Menu,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/features/auth/use-auth";
import type { Role } from "@/types/common";

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  roles?: Role[];
  end?: boolean;
}

const navItems: NavItem[] = [
  { to: "/app", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/app/claims", label: "Claims", icon: FileText },
  { to: "/app/claims/new", label: "New Claim", icon: PlusCircle, roles: ["CLAIMS_OFFICER"] },
  { to: "/app/audit", label: "Audit Log", icon: ScrollText, roles: ["AUDITOR", "ADMIN"] },
  { to: "/app/about", label: "About", icon: Info, roles: ["ADMIN", "CLAIMS_OFFICER"] },
];

function Brand() {
  return (
    <Link to="/app" className="flex items-center gap-2 px-2">
      <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-xs font-bold text-primary-foreground">
        M
      </div>
      <span className="text-sm font-semibold tracking-tight">MMAUG Claims</span>
    </Link>
  );
}

function NavList({ collapsed, onNavigate }: { collapsed: boolean; onNavigate?: () => void }) {
  const { hasRole } = useAuth();

  return (
    <TooltipProvider delayDuration={200}>
      <nav className="flex flex-1 flex-col gap-1 px-2" aria-label="Application">
        {navItems.map((item) => {
          if (item.roles && !hasRole(item.roles)) return null;
          const Icon = item.icon;
          const link = (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-md px-2 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground hover:bg-accent/60 hover:text-accent-foreground",
                  collapsed && "justify-center"
                )
              }
            >
              <Icon className="h-4 w-4 shrink-0" />
              {!collapsed && <span className="truncate">{item.label}</span>}
            </NavLink>
          );

          if (collapsed) {
            return (
              <Tooltip key={item.to}>
                <TooltipTrigger asChild>{link}</TooltipTrigger>
                <TooltipContent side="right">{item.label}</TooltipContent>
              </Tooltip>
            );
          }
          return link;
        })}
      </nav>
    </TooltipProvider>
  );
}

function SidebarContent({ collapsed, onToggle }: { collapsed: boolean; onToggle: () => void }) {
  return (
    <div className="flex h-full flex-col">
      <div className={cn("flex h-16 items-center border-b", collapsed ? "justify-center px-2" : "px-2")}>
        {collapsed ? (
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-xs font-bold text-primary-foreground">
            M
          </div>
        ) : (
          <Brand />
        )}
      </div>

      <div className="flex-1 overflow-y-auto py-4">
        <NavList collapsed={collapsed} />
      </div>

      <div className="border-t p-2">
        <Button
          variant="ghost"
          size={collapsed ? "icon" : "sm"}
          onClick={onToggle}
          className={cn("w-full", collapsed && "w-9")}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <><ChevronLeft className="h-4 w-4" /> Collapse</>}
        </Button>
      </div>
    </div>
  );
}

export function AppSidebar() {
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem("mmaug-sidebar-collapsed") === "true";
  });
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();

  const toggle = () => {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem("mmaug-sidebar-collapsed", String(next));
      return next;
    });
  };

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        className={cn(
          "hidden shrink-0 border-r bg-background transition-[width] duration-200 md:flex md:flex-col",
          collapsed ? "w-16" : "w-60"
        )}
      >
        <SidebarContent collapsed={collapsed} onToggle={toggle} />
      </aside>

      {/* Mobile trigger (rendered inside topbar normally, but fallback here if needed) */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden fixed bottom-4 right-4 z-30 shadow-lg rounded-full"
            aria-label="Open navigation"
            data-mobile-nav-trigger
          >
            <Menu className="h-5 w-5" />
          </Button>
        </SheetTrigger>
        <SheetContent side="left" className="w-64 p-0">
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          <div className="flex h-full flex-col">
            <div className="flex h-16 items-center border-b px-4">
              <Brand />
            </div>
            <div className="flex-1 overflow-y-auto py-4">
              <NavList collapsed={false} onNavigate={() => setMobileOpen(false)} />
            </div>
            <div className="border-t p-4">
              <Badge variant="muted" className="text-[10px]">
                {location.pathname}
              </Badge>
            </div>
          </div>
        </SheetContent>
      </Sheet>
    </>
  );
}