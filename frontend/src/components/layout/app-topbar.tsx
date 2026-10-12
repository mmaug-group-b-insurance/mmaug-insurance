import { Link, useLocation, useNavigate } from "react-router-dom";
import { ChevronRight, LogOut, Moon, Sun, User as UserIcon, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel,
  DropdownMenuSeparator, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/features/auth/use-auth";
import { useTheme } from "@/components/theme-provider";
import { env } from "@/lib/env";

function initials(name: string | undefined): string {
  if (!name) return "?";
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((s) => s[0]?.toUpperCase())
    .join("");
}

const BREADCRUMB_LABELS: Record<string, string> = {
  app: "Dashboard",
  claims: "Claims",
  new: "New Claim",
  audit: "Audit Log",
  about: "About",
  runs: "Agent Run",
};

function useBreadcrumbs() {
  const location = useLocation();
  const parts = location.pathname.split("/").filter(Boolean);
  const crumbs: Array<{ label: string; to: string }> = [];
  let acc = "";
  for (const part of parts) {
    acc += `/${part}`;
    const label = BREADCRUMB_LABELS[part] ?? (/^[0-9a-f-]{8,}$/i.test(part) ? "Detail" : part);
    crumbs.push({ label, to: acc });
  }
  return crumbs;
}

export function AppTopbar() {
  const { account, roles, signOut } = useAuth();
  const { resolved, setTheme } = useTheme();
  const crumbs = useBreadcrumbs();
  const navigate = useNavigate();

  return (
    <header className="sticky top-0 z-30 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
      <div className="flex h-16 items-center gap-4 px-4 sm:px-6">
        <nav aria-label="Breadcrumb" className="min-w-0 flex-1">
          <ol className="flex items-center gap-1.5 text-sm text-muted-foreground">
            {crumbs.map((c, i) => (
              <li key={c.to} className="flex min-w-0 items-center gap-1.5">
                {i > 0 && <ChevronRight className="h-3.5 w-3.5 shrink-0 opacity-60" />}
                {i === crumbs.length - 1 ? (
                  <span className="truncate font-medium text-foreground">{c.label}</span>
                ) : (
                  <Link to={c.to} className="truncate transition-colors hover:text-foreground">
                    {c.label}
                  </Link>
                )}
              </li>
            ))}
          </ol>
        </nav>

        {env.envName !== "Production" && (
          <Badge variant="warning" className="hidden sm:inline-flex">
            <ShieldCheck className="h-3 w-3" /> {env.envName}
          </Badge>
        )}

        <Button
          variant="ghost"
          size="icon"
          onClick={() => setTheme(resolved === "dark" ? "light" : "dark")}
          aria-label="Toggle theme"
        >
          {resolved === "dark" ? <Sun /> : <Moon />}
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2 px-2" aria-label="Account menu">
              <Avatar className="h-8 w-8">
                <AvatarFallback>{initials(account?.name)}</AvatarFallback>
              </Avatar>
              <span className="hidden text-left text-xs leading-tight sm:block">
                <span className="block font-medium">{account?.name ?? "Unknown"}</span>
                <span className="block text-muted-foreground">{roles[0] ?? "—"}</span>
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-64">
            <DropdownMenuLabel>
              <div className="flex flex-col">
                <span className="text-sm font-medium">{account?.name ?? "Unknown user"}</span>
                <span className="text-xs font-normal text-muted-foreground">
                  {account?.username ?? account?.homeAccountId}
                </span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuLabel className="text-[10px] uppercase">Roles</DropdownMenuLabel>
            {roles.length === 0 ? (
              <div className="px-2 pb-2 text-xs text-muted-foreground">No roles assigned</div>
            ) : (
              <div className="flex flex-wrap gap-1 px-2 pb-2">
                {roles.map((r) => (
                  <Badge key={r} variant="muted" className="text-[10px]">{r}</Badge>
                ))}
              </div>
            )}
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate("/app/about")}>
              <UserIcon />
              Profile & system
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => void signOut()}>
              <LogOut />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}