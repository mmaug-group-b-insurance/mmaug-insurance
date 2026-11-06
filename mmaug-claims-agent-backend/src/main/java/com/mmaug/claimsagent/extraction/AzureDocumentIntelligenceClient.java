package com.mmaug.claimsagent.extraction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Set;

/** Bounded Azure OCR adapter. Never follows redirects or returns provider error bodies. */
@Component
@ConditionalOnProperty(name = "app.docintel.provider", havingValue = "azure")
public class AzureDocumentIntelligenceClient implements DocumentIntelligenceClientWrapper {
    private final URI endpoint;
    private final String key;
    private final HttpClient http;
    private static final Set<String> TYPES = Set.of("application/pdf", "image/png", "image/jpeg", "image/tiff");

    @org.springframework.beans.factory.annotation.Autowired
    public AzureDocumentIntelligenceClient(@Value("${app.docintel.endpoint}") String endpoint,
                                           @Value("${app.docintel.key}") String key) {
        this(endpoint, key, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    AzureDocumentIntelligenceClient(String endpoint, String key, HttpClient http) {
        this.endpoint = URI.create(endpoint.replaceAll("/+$", ""));
        if (!"https".equals(this.endpoint.getScheme()) || this.endpoint.getHost() == null
                || this.endpoint.getRawUserInfo() != null || this.endpoint.getRawQuery() != null
                || this.endpoint.getRawFragment() != null || !this.endpoint.getPath().isEmpty()
                || key == null || key.isBlank()) {
            throw new IllegalArgumentException("Azure OCR requires an HTTPS resource endpoint and credential");
        }
        this.key = key;
        this.http = http;
    }

    @Override
    public OcrResult analyse(InputStream document, String contentType) {
        if (!TYPES.contains(contentType == null ? "" : contentType)) return failure("OCR_UNSUPPORTED_TYPE");
        try {
            byte[] bytes = document.readNBytes(15 * 1024 * 1024 + 1);
            if (bytes.length == 0 || bytes.length > 15 * 1024 * 1024) return failure("OCR_INVALID_SIZE");
            URI analyze = URI.create(endpoint + "/documentintelligence/documentModels/prebuilt-layout:analyze?api-version=2024-11-30");
            var response = http.send(request(analyze).header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 202) return failure(status(response.statusCode()));
            URI operation = URI.create(response.headers().firstValue("Operation-Location").orElseThrow());
            // Do not forward the subscription key to another origin or arbitrary resource path.
            if (!endpoint.getScheme().equals(operation.getScheme()) || !endpoint.getHost().equals(operation.getHost())
                    || endpoint.getPort() != operation.getPort() || operation.getRawUserInfo() != null
                    || operation.getRawFragment() != null || !operation.getPath().startsWith(
                    "/documentintelligence/documentModels/prebuilt-layout/analyzeResults/")) return failure("OCR_INVALID_OPERATION");
            long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
            while (System.nanoTime() < deadline) {
                Thread.sleep(1000);
                var poll = http.send(request(operation).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (poll.statusCode() != 200) return failure(status(poll.statusCode()));
                var result = JsonMapper.builder().build().readTree(poll.body());
                String state = result.path("status").asText();
                if ("succeeded".equals(state)) {
                    var pages = result.path("analyzeResult").path("pages");
                    if (!pages.isArray() || pages.isEmpty()) return failure("OCR_INVALID_RESULT");
                    StringBuilder text = new StringBuilder();
                    int expectedPage = 1;
                    for (var page : pages) {
                        if (page.path("pageNumber").asInt() != expectedPage++) return failure("OCR_INVALID_RESULT");
                        if (text.length() > 0) text.append('\f');
                        // Preserve a page boundary even when the preceding page contains no text.
                        else if (expectedPage > 2) text.append('\f');
                        if (!page.path("lines").isArray()) return failure("OCR_INVALID_RESULT");
                        for (var line : page.path("lines")) text.append(line.path("content").asText()).append('\n');
                    }
                    return text.toString().isBlank() ? failure("OCR_NO_TEXT") : OcrResult.ok(text.toString(), pages.size());
                }
                if (!"running".equals(state) && !"notStarted".equals(state)) return failure("OCR_PROVIDER_FAILED");
            }
            return failure("OCR_TIMEOUT");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("OCR_INTERRUPTED");
        } catch (java.net.http.HttpTimeoutException e) {
            return failure("OCR_TIMEOUT");
        } catch (Exception e) {
            return failure("OCR_UNAVAILABLE");
        }
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Ocp-Apim-Subscription-Key", key);
    }
    private static String status(int code) {
        return switch (code) {
            case 401, 403 -> "OCR_ACCESS_DENIED";
            case 429 -> "OCR_RATE_LIMITED";
            default -> "OCR_PROVIDER_FAILED";
        };
    }
    private static OcrResult failure(String code) {
        return OcrResult.failed(code, "OCR could not complete; check service configuration or retry the document");
    }
}
