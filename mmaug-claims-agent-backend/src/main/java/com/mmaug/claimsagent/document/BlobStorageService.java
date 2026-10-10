package com.mmaug.claimsagent.document;

import java.io.InputStream;

public interface BlobStorageService {

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