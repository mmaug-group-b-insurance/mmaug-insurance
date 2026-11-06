package com.mmaug.claimsagent.document;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** On-upload Defender scanning is asynchronous: upload success never implies scan success. */
@Component
@ConditionalOnProperty(name="app.scanner.provider",havingValue="defender-queue")
public class DefenderScanGate implements MalwareScanner {
    @Override public ScanStatus scan(String container,String name){return ScanStatus.PENDING;}
}
