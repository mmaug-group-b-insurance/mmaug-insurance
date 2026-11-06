package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.config.AppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Filesystem-backed blob storage for local development.
 * Production uses Azure Blob Storage.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.storage.provider",havingValue="local",matchIfMissing=true)
@Profile({"local", "test", "dev"})
public class LocalFileStorageService implements BlobStorageService {

    private final Path basePath;

    public LocalFileStorageService(AppProperties props) {
        String configured = props.storage().basePath();
        if (configured == null || configured.isBlank()) {
            configured = "./storage";
        }
        this.basePath = Paths.get(configured).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create storage base path: " + this.basePath, e);
        }
    }

    @Override
    public void store(String container, String blobName, InputStream content, long sizeBytes, String contentType) {
        Path target = resolve(container, blobName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + target, e);
        }
    }

    @Override
    public InputStream retrieve(String container, String blobName) {
        Path path = resolve(container, blobName);
        try {
            if (!Files.exists(path)) {
                throw new NotFoundException("File not found: " + blobName);
            }
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve file: " + path, e);
        }
    }

    @Override
    public void delete(String container, String blobName) {
        Path path = resolve(container, blobName);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file: " + path, e);
        }
    }

    @Override
    public String generateDownloadUrl(String container, String blobName, int expiryMinutes) {
        // Local storage does not issue SAS URLs — the controller streams the file.
        return "/api/v1/documents/" + blobName + "/download";
    }

    private Path resolve(String container, String blobName) {
        Path path = basePath.resolve(container).resolve(blobName).normalize();
        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid blob path");
        }
        return path;
    }
}
