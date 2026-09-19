package com.codemonk.common.arch;

import static com.tngtech.archunit.base.DescribedPredicate.and;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

import com.codemonk.common.exception.DomainException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

/**
 * ArchitectureQualityTest_12
 *
 * <p>Complements {@code ArchitectureQualityTest_13} and
 * {@code ArchitectureQualityTest_15} by pinning down the layering of
 * {@code com.codemonk.common}, the outward dependencies the shared library is
 * allowed to take, and the conventions its exception and logging code follows.
 */
public class ArchitectureQualityTest_12 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Library layers should respect their access boundaries")
    void layersShouldRespectAccessBoundaries() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Constants").definedBy("..constant..")
                .layer("Dto").definedBy("..dto..")
                .layer("Exceptions").definedBy("..exception..")
                .layer("Cache").definedBy("..cache..")
                .layer("Service").definedBy("..service..")
                .whereLayer("Cache").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayNotBeAccessedByAnyLayer()
                .whereLayer("Exceptions").mayOnlyBeAccessedByLayers("Cache", "Service")
                .whereLayer("Dto").mayOnlyBeAccessedByLayers("Exceptions", "Cache", "Service")
                .whereLayer("Constants").mayOnlyBeAccessedByLayers("Dto", "Exceptions", "Cache", "Service")
                .check(importedClasses);
    }

    @Test
    @DisplayName("The shared library should not depend on any downstream CodeMonk module")
    void libraryShouldNotDependOnDownstreamModules() {
        noClasses()
                .that().resideInAPackage(ROOT_PACKAGE + "..")
                .should().dependOnClassesThat(and(
                        resideInAPackage("com.codemonk.."),
                        resideOutsideOfPackage(ROOT_PACKAGE + "..")))
                .because("common-core is consumed by every microservice and must stay standalone")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Spring Web should stay confined to the exception advice and filter layers")
    void springWebShouldBeConfinedToTheExceptionLayer() {
        noClasses()
                .that().resideOutsideOfPackages("..exception..", "..filter..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.web..")
                .because("only the global exception advice and the servlet filters touch the HTTP layer")
                .check(importedClasses);
    }

    @Test
    @DisplayName("DTOs should stay free of framework and persistence dependencies")
    void dtosShouldStayFreeOfFrameworkDependencies() {
        noClasses()
                .that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.apache.kafka..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain exceptions should be unchecked and rooted in DomainException")
    void domainExceptionsShouldBeUncheckedAndRootedInDomainException() {
        classes()
                .that().belongToAnyOf(DomainException.class)
                .should().beAssignableTo(RuntimeException.class)
                .because("callers should never be forced to declare CodeMonk failures")
                .check(importedClasses);

        classes()
                .that().resideInAPackage("..exception..")
                .and().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(DomainException.class)
                .because("a single root keeps the global advice able to map every failure")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Loggers should be declared as private static final fields named 'log'")
    void loggersShouldFollowTheDeclarationConvention() {
        fields()
                .that().haveRawType(Logger.class)
                .should().bePrivate()
                .andShould().beStatic()
                .andShould().beFinal()
                .andShould().haveName("log")
                .check(importedClasses);
    }

    @Test
    @DisplayName("JPA entities should carry the Entity suffix")
    void jpaEntitiesShouldCarryTheEntitySuffix() {
        classes()
                .that().areAnnotatedWith(Entity.class)
                .should().haveSimpleNameEndingWith("Entity")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Production code should not throw generic exceptions")
    void shouldNotThrowGenericExceptions() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(importedClasses);
    }

    @Test
    @DisplayName("Production code should use SLF4J rather than legacy logging APIs")
    void shouldNotUseLegacyLoggingApis() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(importedClasses);
        NO_CLASSES_SHOULD_USE_JODATIME.check(importedClasses);
    }
}
