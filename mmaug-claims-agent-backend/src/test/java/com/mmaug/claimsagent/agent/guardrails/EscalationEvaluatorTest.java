package com.mmaug.claimsagent.agent.guardrails;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.tools.dto.RulesView;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class EscalationEvaluatorTest {
 @Test void usesRetrievedThresholdAndDoesNotSubstituteMissingRules(){
  var claim=new Claim();claim.setEstimatedAmount(new BigDecimal("15000"));var evaluator=new EscalationEvaluator();
  var thresholds=RulesView.Thresholds.builder().highValueAmount(new BigDecimal("20000")).build();
  assertThat(evaluator.evaluate(claim,false,false,thresholds).required()).isFalse();
  thresholds.setHighValueAmount(new BigDecimal("10000"));assertThat(evaluator.evaluate(claim,false,false,thresholds).required()).isTrue();
  assertThat(evaluator.evaluate(claim,false,false,null).required()).isFalse();
  assertThat(evaluator.evaluate(claim,false,true,null).required()).isTrue();
 }
}
