#!/usr/bin/env bash
set -e
ROOT="."
BASE="$ROOT/src/main/java/com/mmaug/claimsagent"
TEST="$ROOT/src/test/java/com/mmaug/claimsagent"
RES="$ROOT/src/main/resources"

mk() { mkdir -p "$(dirname "$1")"; touch "$1"; }

# Root files
for f in pom.xml Dockerfile docker-compose.yml .dockerignore .gitignore .env.example README.md checkstyle.xml spotbugs-exclude.xml; do
  mk "$ROOT/$f"
done

# Main Java files: "package: File1 File2 ..."
while IFS=: read -r pkg files; do
  for f in $files; do mk "$BASE/$pkg/$f.java"; done
done <<'EOF'
.:ClaimsAgentApplication
config:AppProperties AiProperties StorageProperties AiConfig BlobStorageConfig DocumentIntelligenceConfig JpaAuditingConfig AsyncConfig WebConfig JacksonConfig OpenApiConfig ResilienceConfig
security:SecurityConfig JwtAuthConverter Role CurrentUser CurrentUserResolver ClaimAccessPolicy ServiceTokenFilter RateLimitFilter SecurityHeadersFilter CorrelationIdFilter
common:BaseEntity
common/exception:GlobalExceptionHandler ApiError NotFoundException ForbiddenException BusinessValidationException UnsafeFileException ExtractionFailedException ToolUnavailableException AgentGuardrailException
common/util:HashUtil PiiMasker EvidenceRefFormatter ClockProvider
common/crypto:AttributeEncryptor
user:AppUser UserRepository UserService UserController
user/dto:UserResponse
customer:Customer CustomerType CustomerRepository CustomerService
policy:Policy PolicyStatus PolicyCoverage CoverageType Vehicle PolicyDriver HistoricalClaim PolicyRepository PolicyCoverageRepository VehicleRepository HistoricalClaimRepository PolicyService ClaimHistoryService
claim:Claim ClaimStatus ClaimChannel ClaimFact FactStatus ClaimRepository ClaimFactRepository ClaimService ClaimNumberGenerator ClaimMapper ClaimController
claim/dto:CreateClaimRequest UpdateClaimRequest ClaimResponse ClaimListItem ClaimDetailResponse ClaimSearchCriteria
document:ClaimDocument DocumentType ScanStatus ProcessingStatus DocumentRepository DocumentService DocumentController FileValidator MalwareScanner NoOpMalwareScanner BlobStorageService
document/dto:DocumentResponse DocumentUploadResponse
extraction:ExtractionService DocumentIntelligenceClientWrapper StructuringService ExtractionResult ExtractedField ExtractionStatus ExtractionResultRepository ExtractedFieldRepository FactConsolidator
extraction/dto:ClaimFactsExtraction FieldValue ExtractionResponse
validation:CompletenessValidator ContradictionDetector ConfidenceChecker ValidationFinding FindingType Severity FindingOrigin ValidationFindingRepository FindingService FindingController
validation/dto:FindingResponse ResolveFindingRequest
rules:BusinessRulesService RulesLoader RuleSet RuleEvaluationResult EscalationRule
agent:ClaimsAgentOrchestrator AgentRunner AgentRun AgentRunStatus AgentRunRepository AgentStep AgentStepType AgentStepRepository AgentController
agent/prompt:PromptRegistry PromptTemplate PromptContextBuilder
agent/tools:AgentToolRegistry PolicyLookupTool CoverageLookupTool ClaimHistoryTool BusinessRulesTool ToolCallRecorder ToolCall ToolCallStatus ToolCallRepository InternalToolsClient
agent/tools/dto:PolicyToolResult CoverageToolResult ClaimHistoryToolResult BusinessRulesToolResult ToolEnvelope
agent/guardrails:InputSanitizer PromptInjectionDetector OutputSchemaValidator EvidenceVerifier UncertaintyPolicy ForbiddenClaimsFilter EscalationEvaluator
agent/summary:ClaimSummary SummaryStatement EvidenceRef StatementType SourceType ClaimSummaryRepository SummaryStatementRepository SummaryGenerator SummaryService SummaryController
agent/summary/dto:SummaryResponse StatementResponse EvidenceRefResponse
tools:InternalToolsController
tools/dto:PolicyView CoverageView HistoryView RulesView
review:OfficerReview ReviewOutcome OfficerEdit ReviewRepository ReviewService ReviewController
review/dto:CreateReviewRequest ReviewResponse
audit:AuditEvent AuditAction AuditOutcome AuditEventRepository AuditService Audited AuditAspect AuditController
audit/dto:AuditEventResponse
retention:RetentionPolicy RetentionPolicyRepository RetentionJob
EOF

# Resources
for f in application.yml application-local.yml application-dev.yml application-test.yml application-prod.yml logback-spring.xml \
  prompts/manifest.yml prompts/v1/system.txt prompts/v1/extraction.txt prompts/v1/consistency-check.txt prompts/v1/injection-classifier.txt prompts/v1/summary.txt \
  tool-schemas/manifest.yml tool-schemas/v1/get_policy.json tool-schemas/v1/get_coverage.json tool-schemas/v1/get_claim_history.json tool-schemas/v1/get_business_rules.json \
  schemas/claim-facts.schema.json schemas/claim-summary.schema.json schemas/injection-verdict.schema.json \
  rules/business-rules.yaml \
  db/migration/V1__create_users_customers.sql db/migration/V2__create_policies_vehicles_drivers_coverages.sql \
  db/migration/V3__create_historical_claims.sql db/migration/V4__create_claims_and_facts.sql \
  db/migration/V5__create_claim_documents.sql db/migration/V6__create_extraction_tables.sql \
  db/migration/V7__create_validation_findings.sql db/migration/V8__create_agent_runs_steps_tool_calls.sql \
  db/migration/V9__create_summaries_statements_evidence.sql db/migration/V10__create_officer_reviews.sql \
  db/migration/V11__create_audit_events.sql db/migration/V12__create_retention_policies.sql \
  db/migration/V13__indexes_and_constraints.sql \
  db/seed/R__seed_customers.sql db/seed/R__seed_policies.sql db/seed/R__seed_vehicles_drivers.sql \
  db/seed/R__seed_coverages.sql db/seed/R__seed_historical_claims.sql db/seed/R__seed_sample_claims.sql; do
  mk "$RES/$f"
done

# Tests
while IFS=: read -r pkg files; do
  for f in $files; do mk "$TEST/$pkg/$f.java"; done
done <<'EOF'
support:PostgresTestContainer TestJwt StubChatModel StubDocumentIntelligence TestData
security:RoleAccessIT InternalToolsAuthIT SecurityHeadersTest
claim:ClaimServiceTest ClaimControllerIT
document:FileValidatorTest DocumentUploadIT BlobStorageServiceTest
extraction:StructuringServiceTest FactConsolidatorTest
validation:CompletenessValidatorTest ContradictionDetectorTest
rules:BusinessRulesServiceTest
agent:ClaimsAgentOrchestratorIT AgentToolRegistryTest ToolFailureHandlingIT PromptRegistryTest
agent/guardrails:PromptInjectionDetectorTest EvidenceVerifierTest ForbiddenClaimsFilterTest EscalationEvaluatorTest
agent/eval:GoldenClaimsEvalTest InjectionCorpusEvalTest
review:ReviewServiceTest
audit:AuditServiceTest
.:ArchitectureTest
EOF

mkdir -p "$ROOT/src/test/resources/"{golden,injection,documents}
touch "$ROOT/src/test/resources/application-test.yml"

echo "Done: $ROOT"