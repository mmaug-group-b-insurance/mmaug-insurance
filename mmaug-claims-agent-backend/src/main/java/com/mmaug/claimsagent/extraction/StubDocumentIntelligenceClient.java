package com.mmaug.claimsagent.extraction;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Development/test OCR stub.
 * Returns the raw file text (for text-based inputs) and reports success.
 * Never used in production.
 */
@Component
@Profile({"local", "test", "dev"})
public class StubDocumentIntelligenceClient implements DocumentIntelligenceClientWrapper {

    @Override
    public OcrResult analyse(InputStream document, String contentType) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(document, StandardCharsets.UTF_8))) {
            String text = reader.lines().collect(Collectors.joining("\n"));
            int pages = Math.max(1, text.length() / 3000 + 1);
            return OcrResult.ok(text, pages);
        } catch (Exception e) {
            return OcrResult.failed("STUB_OCR_FAILED", e.getMessage());
        }
    }
}