package com.mmaug.claimsagent.review;
import com.mmaug.claimsagent.agent.summary.*;
import com.mmaug.claimsagent.claim.*;
import com.mmaug.claimsagent.review.dto.*;
import com.mmaug.claimsagent.security.*;
import com.mmaug.claimsagent.common.exception.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ReviewVersionIntegrityTest {
 private final ReviewRepository reviews=mock(ReviewRepository.class);private final OfficerEditRepository edits=mock(OfficerEditRepository.class);private final ClaimRepository claims=mock(ClaimRepository.class);private final ClaimSummaryRepository summaries=mock(ClaimSummaryRepository.class);private final SummaryStatementRepository statements=mock(SummaryStatementRepository.class);
 private final ReviewService service=new ReviewService(reviews,edits,claims,summaries,statements);
 private final Claim claim=new Claim();private final ClaimSummary summary=new ClaimSummary();
 private final CurrentUser officer=new CurrentUser(UUID.randomUUID().toString(),"test","officer@example.invalid",Set.of(Role.CLAIMS_OFFICER));
 private CreateReviewRequest request(){claim.setId(UUID.randomUUID());claim.setAssignedOfficerId(UUID.fromString(officer.userId()));summary.setId(UUID.randomUUID());summary.setClaim(claim);when(claims.findForDocumentUpload(claim.getId())).thenReturn(Optional.of(claim));when(summaries.findById(summary.getId())).thenReturn(Optional.of(summary));when(summaries.findFirstByClaimIdOrderByVersionNoDesc(claim.getId())).thenReturn(Optional.of(summary));when(reviews.save(any())).thenAnswer(i->{OfficerReview review=i.getArgument(0);review.setId(UUID.randomUUID());return review;});var request=new CreateReviewRequest();request.setSummaryId(summary.getId());request.setOutcome(ReviewOutcome.ACKNOWLEDGED);request.setComment("Synthetic review");return request;}
 @Test void crossClaimSummaryIsRejectedBeforeSaving(){var request=request();Claim other=new Claim();other.setId(UUID.randomUUID());summary.setClaim(other);assertThatThrownBy(()->service.create(claim.getId(),request,officer)).isInstanceOf(ForbiddenException.class);verifyNoInteractions(reviews,edits);}
 @Test void staleSummaryIsRejected(){var request=request();var latest=new ClaimSummary();latest.setId(UUID.randomUUID());when(summaries.findFirstByClaimIdOrderByVersionNoDesc(claim.getId())).thenReturn(Optional.of(latest));assertThatThrownBy(()->service.create(claim.getId(),request,officer)).isInstanceOf(BusinessValidationException.class);verifyNoInteractions(reviews,edits);}
 @Test void crossVersionStatementIsRejectedBeforeReviewSaved(){var request=request();var st=new SummaryStatement();st.setId(UUID.randomUUID());var other=new ClaimSummary();other.setId(UUID.randomUUID());st.setSummary(other);when(statements.findById(st.getId())).thenReturn(Optional.of(st));var edit=new StatementEdit();edit.setStatementId(st.getId());edit.setEditedText("Correction");edit.setReason("Review note");request.setEdits(List.of(edit));assertThatThrownBy(()->service.create(claim.getId(),request,officer)).isInstanceOf(ForbiddenException.class);verifyNoInteractions(reviews,edits);}
 @Test void correctionPreservesOriginalMachineOutput(){var request=request();var st=new SummaryStatement();st.setId(UUID.randomUUID());st.setSummary(summary);st.setText("Original source-linked text");when(statements.findById(st.getId())).thenReturn(Optional.of(st));var edit=new StatementEdit();edit.setStatementId(st.getId());edit.setEditedText("Officer correction");edit.setReason("Synthetic review note");request.setEdits(List.of(edit));service.create(claim.getId(),request,officer);assertThat(st.getText()).isEqualTo("Original source-linked text");verify(statements,never()).save(any());var capture=org.mockito.ArgumentCaptor.forClass(OfficerEdit.class);verify(edits).save(capture.capture());assertThat(capture.getValue().getOriginalText()).isEqualTo(st.getText());assertThat(capture.getValue().getEditedText()).isEqualTo("Officer correction");}
}
