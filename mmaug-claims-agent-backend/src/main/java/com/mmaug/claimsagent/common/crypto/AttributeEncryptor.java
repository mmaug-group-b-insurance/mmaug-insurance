
package com.mmaug.claimsagent.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES/GCM converter for PII columns.
 *
 * Ciphertext format: Base64( IV (12 bytes) || ciphertext || GCM tag (16 bytes) )
 *
 * In explicit local/dev/test environments, plaintext values that were seeded directly
 * into the database are returned as-is rather than causing decryption to fail.
 * This keeps local development working when the seed SQL inserts plaintext PII.
 */
@Converter
@Component
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String DEVELOPMENT_KEY = "mmaug-dev-only-pii-key-32-bytes!!";

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean lenientOnPlaintext;

    public AttributeEncryptor(
            Environment environment,
            @Value("${app.crypto.pii-key:}") String configuredKey) {

        String[] profiles = environment.getActiveProfiles();
        boolean localOnly = profiles.length > 0 && Arrays.stream(profiles)
                .allMatch(p -> p.equalsIgnoreCase("local") || p.equalsIgnoreCase("dev")
                        || p.equalsIgnoreCase("test"));

        String secret = configuredKey;
        if (secret == null || secret.isBlank()) {
            secret = environment.getProperty("PII_ENCRYPTION_KEY");
        }

        if (secret == null || secret.isBlank()) {
            if (!localOnly) {
                throw new IllegalStateException(
                        "PII_ENCRYPTION_KEY (or app.crypto.pii-key) must be set outside local/dev/test.");
            }
            // Local/dev/test fallback only.
            secret = DEVELOPMENT_KEY;
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (!localOnly && (keyBytes.length != 32 || DEVELOPMENT_KEY.equals(secret))) {
            throw new IllegalStateException("PII encryption requires a non-development key of exactly 32 UTF-8 bytes.");
        }
        // Preserve legacy local key normalization and ciphertext format; this is not a key rotation.
        byte[] normalized = new byte[32];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
        this.keySpec = new SecretKeySpec(normalized, "AES");

        this.lenientOnPlaintext = localOnly;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        // Fast path: try to decode as Base64. If it fails, this is almost
        // certainly a plaintext value inserted by a seed script — return as-is
        // only in explicit local/dev/test profiles.
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(dbData);
        } catch (IllegalArgumentException notBase64) {
            if (lenientOnPlaintext) {
                return dbData;
            }
            throw new IllegalStateException(
                    "Failed to decrypt attribute: value is not valid Base64 ciphertext.", notBase64);
        }

        // Very short payloads cannot be a valid IV + ciphertext + tag.
        if (decoded.length < GCM_IV_LENGTH + GCM_TAG_LENGTH / Byte.SIZE) {
            if (lenientOnPlaintext) {
                return dbData;
            }
            throw new IllegalStateException(
                    "Failed to decrypt attribute: ciphertext too short.");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // The value decoded as Base64 but did not authenticate. That means
            // either tampering, a wrong key, or a value that happened to look
            // like Base64. Only explicit local/dev/test profiles allow this fallback.
            if (lenientOnPlaintext) {
                return dbData;
            }
            throw new IllegalStateException("Failed to decrypt attribute", e);
        }
    }
}
