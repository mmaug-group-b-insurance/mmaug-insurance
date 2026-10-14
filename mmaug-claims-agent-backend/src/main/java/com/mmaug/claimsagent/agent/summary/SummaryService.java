package com.mmaug.claimsagent.agent.summary;

import com.mmaug.claimsagent.agent.summary.dto.EvidenceRefResponse;
import com.mmaug.claimsagent.agent.summary.dto.StatementResponse;
import com.mmaug.claimsagent.agent.summary.dto.SummaryResponse;
import com.mmaug.claimsagent.agent.summary.dto.SummaryVersionItem;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SummaryService {

    private static final String DISCLAIMER =
            "Decision-support only. This summary does not approve, reject, "
                    + "determine liability, authorise payment, or modify any policy.";

    private final ClaimSummaryRepository summaryRepository;
    private final SummaryStatementRepository statementRepository;
    private final EvidenceRefRepository evidenceRefRepository;

    public SummaryService(ClaimSummaryRepository summaryRepository,
                          SummaryStatementRepository statementRepository,
                          EvidenceRefRepository evidenceRefRepository) {
        this.summaryRepository = summaryRepository;
        this.statementRepository = statementRepository;
        this.evidenceRefRepository = evidenceRefRepository;
    }

    public SummaryResponse getLatest(UUID claimId) {
        ClaimSummary summary = summaryRepository.findFirstByClaimIdOrderByVersionNoDesc(claimId)
                .orElseThrow(() -> new NotFoundException("No summary for claim: " + claimId));
        return toResponse(summary);
    }

    public SummaryResponse getVersion(UUID claimId, Integer versionNo) {
        if (versionNo == null) return getLatest(claimId);
        ClaimSummary summary = summaryRepository.findAllByClaimIdOrderByVersionNoDesc(claimId).stream()
                .filter(s -> s.getVersionNo().equals(versionNo))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Summary version not found"));
        return toResponse(summary);
    }

    public List<SummaryVersionItem> listVersions(UUID claimId) {
        return summaryRepository.findAllByClaimIdOrderByVersionNoDesc(claimId).stream()
                .map(s -> SummaryVersionItem.builder()
                        .summaryId(s.getId())
                        .versionNo(s.getVersionNo())
                        .generatedAt(s.getGeneratedAt())
                        .headline(s.getHeadline())
                        .overallConfidence(s.getOverallConfidence())
                        .build())
                .toList();
    }

    private SummaryResponse toResponse(ClaimSummary summary) {
        List<SummaryStatement> statements =
                statementRepository.findAllBySummaryIdOrderByOrderNoAsc(summary.getId());

        Map<String, List<StatementResponse>> sections = new LinkedHashMap<>();
        for (SummaryStatement st : statements) {
            StatementResponse dto = StatementResponse.builder()
                    .id(st.getId())
                    .orderNo(st.getOrderNo())
                    .section(st.getSection())
                    .type(st.getStatementType().name())
                    .text(st.getText())
                    .confidence(st.getConfidence())
                    .evidence(toEvidenceDtos(st.getId()))
                    .build();
            sections.computeIfAbsent(st.getSection(), k -> new ArrayList<>()).add(dto);
        }

        return SummaryResponse.builder()
                .summaryId(summary.getId())
                .claimId(summary.getClaim().getId())
                .versionNo(summary.getVersionNo())
                .headline(summary.getHeadline())
                .overallConfidence(summary.getOverallConfidence())
                .escalationRecommended(summary.getEscalationRecommended())
                .escalationReasons(parseJsonList(summary.getEscalationReasons()))
                .uncertaintyNotes(parseJsonList(summary.getUncertaintyNotes()))
                .disclaimer(summary.getDisclaimer())
                .generatedAt(summary.getGeneratedAt())
                .promptVersion(summary.getPromptVersion())
                .llmModel(summary.getLlmModel())
                .sections(sections)
                .build();
    }

    private List<EvidenceRefResponse> toEvidenceDtos(UUID statementId) {
        return evidenceRefRepository.findAllByStatementId(statementId).stream()
                .map(e -> EvidenceRefResponse.builder()
                        .sourceType(e.getSourceType().name())
                        .documentId(e.getDocumentId())
                        .page(e.getPageNumber())
                        .snippet(e.getSnippet())
                        .toolCallId(e.getToolCallId())
                        .ruleId(e.getRuleId())
                        .fieldName(e.getFieldName())
                        .build())
                .toList();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        String stripped = json.trim();
        if (stripped.startsWith("[") && stripped.endsWith("]")) {
            stripped = stripped.substring(1, stripped.length() - 1);
            if (stripped.isBlank()) return List.of();
            List<String> out = new ArrayList<>();
            for (String part : stripped.split(",")) {
                String clean = part.trim().replaceAll("^\"|\"$", "");
                if (!clean.isBlank()) out.add(clean);
            }
            return out;
        }
        return List.of(json);
    }

    public String disclaimer() {
        return DISCLAIMER;
    }
}