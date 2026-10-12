import { apiRequest } from "@/lib/api-client";
import type { Extraction } from "@/types/extraction";

export const extractionService = {
  get(documentId: string, signal?: AbortSignal) {
    return apiRequest<Extraction>(`/documents/${documentId}/extraction`, { signal });
  },
};