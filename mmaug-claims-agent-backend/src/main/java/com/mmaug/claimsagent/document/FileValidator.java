package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.common.exception.UnsafeFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15 MB
    public static final int MAX_FILES_PER_CLAIM = 20;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png", "tiff", "tif");

    // Magic byte signatures for each supported type
    private static final Map<String, List<byte[]>> MAGIC = Map.of(
            "pdf", List.of(new byte[]{0x25, 0x50, 0x44, 0x46}),                     // %PDF
            "jpg", List.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),      // JPEG
            "png", List.of(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}),              // PNG
            "tif", List.of(
                    new byte[]{0x49, 0x49, 0x2A, 0x00},                             // TIFF (LE)
                    new byte[]{0x4D, 0x4D, 0x00, 0x2A})                             // TIFF (BE)
    );

    public void validate(MultipartFile file, long existingDocumentCount) {
        if (file == null || file.isEmpty()) {
            throw new UnsafeFileException("File is empty");
        }

        if (existingDocumentCount >= MAX_FILES_PER_CLAIM) {
            throw new UnsafeFileException("Maximum of " + MAX_FILES_PER_CLAIM + " documents per claim exceeded");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new UnsafeFileException("File exceeds the 15 MB limit");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new UnsafeFileException("File name is missing");
        }

        String extension = extractExtension(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsafeFileException("Unsupported file type. Allowed: PDF, JPG, JPEG, PNG, TIFF");
        }

        byte[] head = readHead(file, 16);
        String normalizedExt = "jpeg".equals(extension) ? "jpg" : extension;
        if ("tif".equals(normalizedExt)) {
            normalizedExt = "tif";
        }

        if (!matchesMagic(head, normalizedExt)) {
            throw new UnsafeFileException("File content does not match the declared extension");
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new UnsafeFileException("File has no extension");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readHead(MultipartFile file, int max) {
        try (InputStream in = file.getInputStream()) {
            byte[] buf = new byte[max];
            int read = in.read(buf);
            if (read <= 0) {
                throw new UnsafeFileException("Unable to read file contents");
            }
            if (read < max) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buf, 0, trimmed, 0, read);
                return trimmed;
            }
            return buf;
        } catch (IOException e) {
            throw new UnsafeFileException("Failed to read file contents: " + e.getMessage());
        }
    }

    private boolean matchesMagic(byte[] head, String extension) {
        List<byte[]> signatures = MAGIC.get(extension);
        if (signatures == null) return false;
        for (byte[] sig : signatures) {
            if (startsWith(head, sig)) return true;
        }
        return false;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}