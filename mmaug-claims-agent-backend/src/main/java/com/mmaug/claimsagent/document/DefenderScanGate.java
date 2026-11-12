package com.mmaug.claimsagent.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** On-upload Defender scanning is asynchronous: upload success never implies scan success. */
@Component
@ConditionalOnProperty(name="app.scanner.provider",havingValue="defender-queue")
public class DefenderScanGate implements MalwareScanner {
    private final boolean autoClean;

    public DefenderScanGate() {
        this(false);
    }

    public DefenderScanGate(@Value("${app.scanner.auto-clean:false}") boolean autoClean) {
        this.autoClean = autoClean;
    }

    @Override
    public ScanStatus scan(String container, String name) {
        return autoClean ? ScanStatus.CLEAN : ScanStatus.PENDING;
    }
}
