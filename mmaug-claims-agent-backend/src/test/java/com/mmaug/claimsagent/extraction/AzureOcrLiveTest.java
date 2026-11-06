package com.mmaug.claimsagent.extraction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: makes one potentially billable call with generated synthetic evidence only. */
@EnabledIfEnvironmentVariable(named = "MMAUG_RUN_LIVE_OCR", matches = "true")
class AzureOcrLiveTest {
    @Test void readsGeneratedSyntheticClaimImage() throws Exception {
        String endpoint = System.getenv("DOCINTEL_ENDPOINT");
        String key = System.getenv("DOCINTEL_KEY");
        assertNotNull(endpoint, "Configure DOCINTEL_ENDPOINT in the test process");
        assertNotNull(key, "Configure DOCINTEL_KEY securely in the test process");
        BufferedImage image = new BufferedImage(1800, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
            graphics.drawString("SYNTHETIC TRAINING DOCUMENT - NOT A REAL CLAIM", 60, 100);
            graphics.drawString("Policy number: SYN-48271", 60, 220);
            graphics.drawString("Vehicle registration: TEST-123", 60, 340);
            graphics.drawString("Incident date: 2026-10-20", 60, 460);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", bytes));
        var result = new AzureDocumentIntelligenceClient(endpoint, key)
                .analyse(new ByteArrayInputStream(bytes.toByteArray()), "image/png");
        assertTrue(result.success(), () -> "Azure OCR failed: " + result.errorCode());
        assertEquals(1, result.pageCount());
        assertTrue(result.rawText().contains("SYN-48271"), "Synthetic policy number was not recovered");
        assertTrue(result.rawText().contains("TEST-123"), "Synthetic registration was not recovered");
    }
}
