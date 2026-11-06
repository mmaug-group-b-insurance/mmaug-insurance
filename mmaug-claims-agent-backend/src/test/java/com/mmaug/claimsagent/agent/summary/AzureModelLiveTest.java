package com.mmaug.claimsagent.agent.summary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** One potentially billable request with invented test statements; no real claim files. */
@EnabledIfEnvironmentVariable(named="MMAUG_RUN_LIVE_MODEL",matches="true")
class AzureModelLiveTest {
    @Test void organizesSyntheticStatementsWithoutChangingTheirTextOrCitations() {
        String endpoint=System.getenv("AI_ENDPOINT"), key=System.getenv("AI_KEY"), deployment=System.getenv("AI_DEPLOYMENT");
        assertThat(endpoint).as("Configure AI_ENDPOINT").isNotBlank();
        assertThat(key).as("Configure AI_KEY privately in this PowerShell process").isNotBlank();
        assertThat(deployment).as("Configure AI_DEPLOYMENT").isNotBlank();
        var reference=new GroundedStatement.Citation(SourceType.DOCUMENT,UUID.randomUUID(),UUID.randomUUID(),1,"SYN123",null,"vehicleRegistration","SYN123");
        var statements=List.of(new GroundedStatement("INCIDENT",StatementType.EXTRACTED_FACT,
                GroundedStatement.documentText("vehicleRegistration","SYN123",false),null,List.of(reference)),
                new GroundedStatement("INCIDENT",StatementType.UNCERTAIN,
                        GroundedStatement.documentText("vehicleRegistration","TEST999",true),null,
                        List.of(new GroundedStatement.Citation(SourceType.DOCUMENT,UUID.randomUUID(),UUID.randomUUID(),1,"TEST999",null,"vehicleRegistration","TEST999"))));
        var draft=new GroundedSummaryBuilder.Draft("Synthetic provider smoke test","LOW",statements,List.of("Synthetic test input; not a real claim"));
        var result=new AzureClaimAnalysis(endpoint,key,deployment).organize(draft);
        assertThat(result.model()).isEqualTo("azure-openai/"+deployment);
        assertThat(result.draft().statements().stream().filter(st->!st.section().equals("OVERVIEW")).toList())
                .containsExactlyInAnyOrderElementsOf(statements);
        assertThat(result.draft().statements()).allSatisfy(st->{
            assertThat(statements).anySatisfy(original->{
                assertThat(st.text()).isEqualTo(original.text());
                assertThat(st.citations()).isEqualTo(original.citations());
            });
        });
    }
}
