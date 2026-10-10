package com.mmaug.claimsagent.extraction;

import java.io.InputStream;

/**
 * Abstraction over an OCR / document intelligence provider.
 * Production uses Azure Document Intelligence. Test and dev use a stub.
 */
public interface DocumentIntelligenceClientWrapper {

    OcrResult analyse(InputStream document, String contentType);

    record OcrResult(String rawText, int pageCount, boolean success, String errorCode, String errorMessage) {

        public static OcrResult ok(String rawText, int pageCount) {
            return new OcrResult(rawText, pageCount, true, null, null);
        }

        public static OcrResult failed(String errorCode, String errorMessage) {
            return new OcrResult(null, 0, false, errorCode, errorMessage);
        }
    }
}