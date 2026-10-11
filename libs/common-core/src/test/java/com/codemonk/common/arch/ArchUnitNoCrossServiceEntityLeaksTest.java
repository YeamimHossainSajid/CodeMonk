package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ArchUnitNoCrossServiceEntityLeaksTest
 *
 * <p>common-core is the library every service depends on, so anything it exposes is
 * effectively shared across services. These rules keep JPA entities inside the
 * {@code service} package that owns them, so that responses, service contracts and wire
 * types (dto, event) never carry a persistence-backed class into another service.
 */
public class ArchUnitNoCrossServiceEntityLeaksTest {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private static final String SERVICE_PACKAGE = "..service..";

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("JPA entities should only be used inside the service package")
    void entitiesShouldNotBeUsedOutsideTheServicePackage() {
        noClasses()
                .that().resideOutsideOfPackage(SERVICE_PACKAGE)
                .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                .because("an entity that escapes its owning package ends up in another service's contract")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Entity base classes should only be used inside the service package")
    void entityBaseClassesShouldNotBeUsedOutsideTheServicePackage() {
        noClasses()
                .that().resideOutsideOfPackage(SERVICE_PACKAGE)
                .should().dependOnClassesThat().areAnnotatedWith(MappedSuperclass.class)
                .because("a mapped superclass carries the same persistence mapping as the entities that extend it")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Response types should not depend on JPA entities")
    void responseTypesShouldNotDependOnEntities() {
        noClasses()
                .that().haveSimpleNameEndingWith("Response")
                .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                .because("a response is copied out of an entity by a mapper, never wrapped around one")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Service types should not depend on JPA entities")
    void serviceTypesShouldNotDependOnEntities() {
        noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                .because("a service contract returns responses, so callers in other services never see an entity")
                .check(importedClasses);
    }

    @Test
    @DisplayName("JPA entities should not depend on dto or event types")
    void entitiesShouldNotDependOnWireContracts() {
        noClasses()
                .that().areAnnotatedWith(Entity.class)
                .or().areAnnotatedWith(MappedSuperclass.class)
                .should().dependOnClassesThat().resideInAnyPackage("..dto..", "..event..")
                .because("the persistence model must not be shaped by the contracts shared between services")
                .check(importedClasses);
    }
}