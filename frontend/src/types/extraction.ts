/** Mirrors the Spring Boot ExtractionResponse; no client-side inference is added. */
export interface ExtractedField {
  fieldName: string;
  value: string;
  normalisedValue: string | null;
  confidence: number;
  page: number | null;
  snippet: string | null;
}

export interface Extraction {
  extractionId: string;
  documentId: string;
  status: "PENDING" | "SUCCEEDED" | "PARTIAL" | "FAILED";
  extractorModel: string;
  llmModel: string | null;
  promptVersion: string;
  schemaVersion: string;
  overallConfidence: number | null;
  injectionSuspected: boolean;
  fields: ExtractedField[];
}
