package com.mmaug.claimsagent;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the Responsible AI boundary: the agent package cannot import
 * classes that write, approve, reject, pay or modify policy.
 */
class ArchitectureTest {

    @Test
    void agentPackageDoesNotImportWriteServices() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.mmaug.claimsagent");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..agent..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..policy.PolicyService",
                        "..claim.ClaimService"
                );

        rule.check(classes);
    }
}