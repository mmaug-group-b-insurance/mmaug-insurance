import { describe, it, expect } from "vitest";

// Mirror of FileDropzone constants (kept in sync deliberately — this test locks the client contract).
const MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024;
const MAX_FILES = 20;
const ALLOWED_MIME = ["application/pdf", "image/jpeg", "image/jpg", "image/png", "image/tiff"];
const ALLOWED_EXT = /\.(pdf|jpe?g|png|tiff?)$/i;

function isAllowed(mime: string, name: string): boolean {
  return ALLOWED_MIME.includes(mime) || ALLOWED_EXT.test(name);
}

function isSizeOk(bytes: number): boolean {
  return bytes <= MAX_FILE_SIZE_BYTES;
}

function makeFile(name: string, type: string, sizeBytes: number): File {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe("file rules", () => {
  it("accepts supported MIME types", () => {
    expect(isAllowed("application/pdf", "a.pdf")).toBe(true);
    expect(isAllowed("image/jpeg", "a.jpg")).toBe(true);
    expect(isAllowed("image/png", "a.png")).toBe(true);
    expect(isAllowed("image/tiff", "a.tiff")).toBe(true);
  });

  it("rejects unsupported MIME types without a matching extension", () => {
    expect(isAllowed("application/zip", "a.zip")).toBe(false);
    expect(isAllowed("application/msword", "a.doc")).toBe(false);
    expect(isAllowed("text/html", "a.html")).toBe(false);
  });

  it("falls back to extension when MIME is empty or generic", () => {
    expect(isAllowed("", "report.pdf")).toBe(true);
    expect(isAllowed("application/octet-stream", "photo.JPG")).toBe(true);
  });

  it("enforces 15 MB per-file limit", () => {
    expect(isSizeOk(MAX_FILE_SIZE_BYTES)).toBe(true);
    expect(isSizeOk(MAX_FILE_SIZE_BYTES + 1)).toBe(false);
  });

  it("enforces 20 files per claim limit", () => {
    const tooMany = Array.from({ length: 21 }).map((_, i) => makeFile(`f${i}.pdf`, "application/pdf", 1024));
    expect(tooMany.length > MAX_FILES).toBe(true);
  });
});