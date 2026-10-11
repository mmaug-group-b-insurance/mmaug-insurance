package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.common.exception.UnsafeFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    private final FileValidator validator = new FileValidator();

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file, 0))
                .isInstanceOf(UnsafeFileException.class);
    }

    @Test
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe",
                "application/octet-stream", new byte[]{0x25, 0x50, 0x44, 0x46});
        assertThatThrownBy(() -> validator.validate(file, 0))
                .isInstanceOf(UnsafeFileException.class);
    }

    @Test
    void rejectsMismatchedMagicBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "not a pdf".getBytes());
        assertThatThrownBy(() -> validator.validate(file, 0))
                .isInstanceOf(UnsafeFileException.class);
    }

    @Test
    void rejectsTooManyFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        assertThatThrownBy(() -> validator.validate(file, 20))
                .isInstanceOf(UnsafeFileException.class);
    }
}