package com.mmaug.claimsagent.common.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AttributeEncryptorTest {
    private static final String KEY = "0123456789abcdef0123456789abcdef"; // gitleaks:allow -- fixed synthetic AES test vector

    private MockEnvironment environment(String... profiles) {
        var env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "+12025550123", "example@example.invalid", "Synthetic O'Brien – Żgħażagħ"})
    void shortAndUnicodeValuesRoundTripInLocalAndStaging(String value) {
        for (String profile : new String[]{"local", "staging"}) {
            var converter = new AttributeEncryptor(environment(profile), KEY);
            String first = converter.convertToDatabaseColumn(value);
            assertThat(first).isNotEqualTo(value);
            assertThat(converter.convertToEntityAttribute(first)).isEqualTo(value);
            assertThat(converter.convertToDatabaseColumn(value)).isNotEqualTo(first);
        }
    }

    @Test void readsExistingUnprefixedCiphertextWithoutMigration() throws Exception {
        byte[] iv = new byte[12]; // Fixed test vector only; application encryption uses SecureRandom.
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal("Legacy synthetic".getBytes(StandardCharsets.UTF_8));
        String stored = Base64.getEncoder().encodeToString(ByteBuffer.allocate(12 + encrypted.length)
                .put(iv).put(encrypted).array());
        assertThat(new AttributeEncryptor(environment("staging"), KEY).convertToEntityAttribute(stored))
                .isEqualTo("Legacy synthetic");
    }

    @Test void sharedProfilesRequireExplicitValidKeyEvenWhenMixedWithLocal() {
        for (String[] profiles : new String[][]{{}, {"staging"}, {"prod"}, {"production"}, {"staging", "local"}, {"preview"}}) {
            var env = environment(profiles);
            for (String invalid : new String[]{"", "short", KEY + "extra", "mmaug-dev-only-pii-key-32-bytes!!"}) {
                assertThatThrownBy(() -> new AttributeEncryptor(env, invalid))
                        .isInstanceOf(IllegalStateException.class);
            }
            assertThatCode(() -> new AttributeEncryptor(env, KEY)).doesNotThrowAnyException();
        }
    }

    @Test void environmentKeyIsSupportedAndNullsStayNull() {
        var converter = new AttributeEncryptor(environment("staging").withProperty("PII_ENCRYPTION_KEY", KEY), "");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test void stagingRejectsPlaintextTruncationTamperingAndWrongKey() {
        var converter = new AttributeEncryptor(environment("staging"), KEY);
        String encrypted = converter.convertToDatabaseColumn("Synthetic name");
        byte[] tampered = Base64.getDecoder().decode(encrypted);
        tampered[tampered.length - 1] ^= 1;
        for (String invalid : new String[]{"Plain synthetic name", "YWJj", Base64.getEncoder().encodeToString(tampered)}) {
            assertThatThrownBy(() -> converter.convertToEntityAttribute(invalid)).isInstanceOf(IllegalStateException.class);
        }
        var otherKey = new AttributeEncryptor(environment("staging"), "fedcba9876543210fedcba9876543210");
        assertThatThrownBy(() -> otherKey.convertToEntityAttribute(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test void explicitLocalProfilesKeepLegacySeedCompatibility() {
        for (String profile : new String[]{"local", "dev", "test"}) {
            var converter = new AttributeEncryptor(environment(profile), "");
            assertThat(converter.convertToEntityAttribute("Synthetic seed name")).isEqualTo("Synthetic seed name");
            assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn("A"))).isEqualTo("A");
        }
    }
}
