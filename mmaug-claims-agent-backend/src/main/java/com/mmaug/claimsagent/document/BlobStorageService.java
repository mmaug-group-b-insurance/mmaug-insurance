package com.mmaug.claimsagent.document;

import java.io.InputStream;

public interface BlobStorageService {
    /** Opaque immutable storage version; local storage has no external ETag. */
    default String version(String container, String blobName) { return null; }

    /** Read an exact external version. Implementations without version support must fail closed. */
    default InputStream retrieveVersion(String container, String blobName, String etag) {
        throw new UnsupportedOperationException("Version-pinned storage is unavailable");
    }

    /**
     * Store a file in the specified logical container.
     *
     * @param container logical container (e.g. "quarantine", "clean")
     * @param blobName  server-generated name (never user-provided)
     * @param content   input stream
     * @param sizeBytes content length
     * @param contentType MIME type
     */
    void store(String container, String blobName, InputStream content, long sizeBytes, String contentType);

    /**
     * Retrieve a file as a stream.
     */
    InputStream retrieve(String container, String blobName);

    /**
     * Delete a file.
     */
    void delete(String container, String blobName);

    /**
     * Generate a short-lived download URL (implementation-specific).
     * For local storage, returns a path; for Azure, returns a SAS URL.
     */
    String generateDownloadUrl(String container, String blobName, int expiryMinutes);
}
