package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ArchitectureQualityTest_13
 *
 * <p>Enforces package encapsulation, layering boundaries and naming/structure
 * conventions across the {@code com.codemonk.common} library.
 */
public class ArchitectureQualityTest_13 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Constant holders should not depend on any other common-core package")
    void constantsShouldBeSelfContained() {
        noClasses()
                .that().resideInAPackage("..constant..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..cache..", "..dto..", "..exception..", "..service..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Constant holders should be final and expose only final fields")
    void constantHoldersShouldBeFinalAndImmutable() {
        classes()
                .that().resideInAPackage("..constant..")
                .should().haveModifier(JavaModifier.FINAL)
                .andShould().haveOnlyFinalFields()
                .check(importedClasses);
    }

    @Test
    @DisplayName("DTOs should be records and stay free of infrastructure dependencies")
    void dtosShouldBeRecordsWithoutInfrastructureDependencies() {
        classes()
                .that().resideInAPackage("..dto..")
                .should().beRecords()
                .check(importedClasses);

        noClasses()
                .that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..cache..", "..service..", "..exception..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception layer should not depend on the cache layer")
    void exceptionLayerShouldNotDependOnCacheLayer() {
        noClasses()
                .that().resideInAPackage("..exception..")
                .should().dependOnClassesThat()
                .resideInAPackage("..cache..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Spring stereotypes should live in the cache, service or exception packages")
    void springStereotypesShouldResideInAllowedPackages() {
        classes()
                .that().areAnnotatedWith(Service.class)
                .or().areAnnotatedWith(Component.class)
                .or().areAnnotatedWith(RestControllerAdvice.class)
                .should().resideInAnyPackage("..cache..", "..service..", "..exception..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Dependencies should be constructor-injected, never field-injected")
    void shouldNotUseFieldInjection() {
        noFields()
                .should().beAnnotatedWith(Autowired.class)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Production code should not write to standard streams")
    void shouldNotAccessStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(importedClasses);
    }

    @Test
    @DisplayName("Packages should be free of cyclic dependencies")
    void packagesShouldBeFreeOfCycles() {
        slices()
                .matching(ROOT_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(importedClasses);
    }
}
