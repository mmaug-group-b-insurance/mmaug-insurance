package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.claim.*;
import com.mmaug.claimsagent.document.ClaimDocument;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FactConsolidatorTest {
    private final ExtractedFieldRepository fields = mock(ExtractedFieldRepository.class);
    private final ClaimFactRepository facts = mock(ClaimFactRepository.class);
    private final FactConsolidator service = new FactConsolidator(fields, facts);

    @Test void retainsBothConflictingSourcesAndDoesNotDuplicateOnRetry() {
        Claim claim = new Claim(); claim.setId(UUID.randomUUID());
        ClaimDocument doc = new ClaimDocument(); doc.setId(UUID.randomUUID()); doc.setClaim(claim);
        ClaimFact original = new ClaimFact(); original.setFieldName("vehicleRegistration");
        original.setValueText("SYN111"); original.setSourceDocumentId(UUID.randomUUID());
        original.setSourceSnippet("SYN111"); original.setStatus(FactStatus.CONFIRMED);
        ExtractedField incoming = new ExtractedField(); incoming.setDocument(doc);
        incoming.setFieldName("vehicleRegistration"); incoming.setFieldValue("SYN222");
        incoming.setPageNumber(2); incoming.setSnippet("SYN222");
        UUID extraction = UUID.randomUUID();
        when(fields.findAllByExtractionId(extraction)).thenReturn(List.of(incoming));
        when(facts.findAllByClaimId(claim.getId())).thenReturn(List.of(original));
        service.consolidate(claim, extraction);
        String alternatives = original.getAlternateValues();
        service.consolidate(claim, extraction);
        assertThat(original.getStatus()).isEqualTo(FactStatus.CONFLICTING);
        assertThat(original.getValueText()).isEqualTo("SYN111");
        assertThat(original.getAlternateValues()).isEqualTo(alternatives)
                .contains("SYN222", doc.getId().toString(), extraction.toString(), "\"page\":2");
    }

    @Test void rejectsCrossCaseSourcesBeforeWritingAnyFacts() {
        Claim claim = new Claim(); claim.setId(UUID.randomUUID());
        Claim other = new Claim(); other.setId(UUID.randomUUID());
        ClaimDocument doc = new ClaimDocument(); doc.setClaim(other);
        ExtractedField field = new ExtractedField(); field.setDocument(doc);
        UUID extraction = UUID.randomUUID();
        when(fields.findAllByExtractionId(extraction)).thenReturn(List.of(field));
        when(facts.findAllByClaimId(claim.getId())).thenReturn(List.of());
        assertThatThrownBy(() -> service.consolidate(claim, extraction)).isInstanceOf(IllegalArgumentException.class);
        verify(facts, never()).save(any());
    }
}
