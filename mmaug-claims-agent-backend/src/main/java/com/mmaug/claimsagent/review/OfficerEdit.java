package com.mmaug.claimsagent.review;

import com.mmaug.claimsagent.agent.summary.SummaryStatement;
import com.mmaug.claimsagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "officer_edit")
@Getter
@Setter
@NoArgsConstructor
public class OfficerEdit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private OfficerReview review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private SummaryStatement statement;

    @Column(name = "original_text", length = 1500)
    private String originalText;

    @Column(name = "edited_text", length = 1500)
    private String editedText;

    @Column(name = "reason", length = 500)
    private String reason;
}