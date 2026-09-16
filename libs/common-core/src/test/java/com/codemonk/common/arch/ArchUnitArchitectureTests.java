package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.codemonk.common.exception.DomainException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ArchUnitArchitectureTests
 *
 * <p>Enforces clean architecture layer boundaries across {@code com.codemonk.common}.
 * Packages are grouped into concentric rings, and dependencies may only point inwards:
 * <ul>
 *   <li><b>Foundation</b> ({@code constant}, {@code dto}): framework-free contracts</li>
 *   <li><b>Domain</b> ({@code exception}): failure model and its HTTP translation</li>
 *   <li><b>Infrastructure</b> ({@code cache}, {@code service}): Redis, Kafka and JPA adapters</li>
 * </ul>
 *
 * <p>Complements the per-package layering in {@code ArchitectureQualityTest_12} by checking
 * the direction of dependencies between rings and the isolation of sibling adapters.
 */
public class ArchUnitArchitectureTests {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private static final String[] FOUNDATION_PACKAGES = {"..constant..", "..dto.."};
    private static final String[] DOMAIN_PACKAGES = {"..exception.."};
    private static final String[] INFRASTRUCTURE_PACKAGES = {"..cache..", "..service.."};

    private static final String[] INFRASTRUCTURE_FRAMEWORK_PACKAGES = {
            "org.springframework.data..",
            "org.springframework.dao..",
            "org.springframework.jdbc..",
            "org.springframework.kafka..",
            "org.apache.kafka..",
            "jakarta.persistence..",
            "org.hibernate.."
    };

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Dependencies between architecture rings should only point inwards")
    void ringDependenciesShouldPointInwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Foundation").definedBy(FOUNDATION_PACKAGES)
                .layer("Domain").definedBy(DOMAIN_PACKAGES)
                .layer("Infrastructure").definedBy(INFRASTRUCTURE_PACKAGES)
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Foundation").mayOnlyBeAccessedByLayers("Domain", "Infrastructure")
                .whereLayer("Foundation").mayNotAccessAnyLayer()
                .whereLayer("Domain").mayOnlyAccessLayers("Foundation")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure adapters should not depend on each other")
    void infrastructureAdaptersShouldBeIsolated() {
        noClasses()
                .that().resideInAPackage("..cache..")
                .should().dependOnClassesThat().resideInAPackage("..service..")
                .because("the Redis adapter must be usable without pulling in Kafka or JPA adapters")
                .check(importedClasses);

        noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..cache..")
                .because("service adapters must be usable without a Redis connection")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Inner rings should not depend on infrastructure frameworks")
    void innerRingsShouldNotDependOnInfrastructureFrameworks() {
        noClasses()
                .that().resideInAnyPackage(FOUNDATION_PACKAGES)
                .or().resideInAnyPackage(DOMAIN_PACKAGES)
                .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE_FRAMEWORK_PACKAGES)
                .because("persistence and messaging technologies belong in the infrastructure ring")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Foundation contracts should not depend on Spring or Jakarta")
    void foundationShouldNotDependOnSpringOrJakarta() {
        noClasses()
                .that().resideInAnyPackage(FOUNDATION_PACKAGES)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
                .because("constants and DTOs are shared by every service regardless of its runtime stack")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain exceptions should be transport-agnostic")
    void domainExceptionsShouldBeTransportAgnostic() {
        noClasses()
                .that().resideInAnyPackage(DOMAIN_PACKAGES)
                .and().areAssignableTo(DomainException.class)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.servlet..")
                .because("only the global advice may translate domain failures into HTTP responses")
                .check(importedClasses);
    }

    @Test
    @DisplayName("The web advice should only be referenced from within the domain ring")
    void webAdviceShouldOnlyBeReferencedFromDomainRing() {
        classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .should().onlyHaveDependentClassesThat().resideInAnyPackage(DOMAIN_PACKAGES)
                .because("the advice is an entry point wired by Spring, not a collaborator to call directly")
                .check(importedClasses);
    }
}
