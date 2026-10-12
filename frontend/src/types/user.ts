import type { Role } from "./common";

export interface User {
  id: string;
  displayName: string;
  email: string;
  roles: Role[];
  customerId?: string | null;
}

export interface Versions {
  appVersion: string;
  gitCommit: string;
  promptVersion: string;
  toolSchemaVersion: string;
  ruleSetVersion: string;
  ruleSetSha256: string;
}