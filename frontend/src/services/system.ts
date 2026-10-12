import { apiRequest } from "@/lib/api-client";
import type { User, Versions } from "@/types/user";

export const systemService = {
  me(signal?: AbortSignal) {
    return apiRequest<User>("/me", { signal });
  },
  versions(signal?: AbortSignal) {
    return apiRequest<Versions>("/admin/versions", { signal });
  },
};