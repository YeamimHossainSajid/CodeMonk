package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ArchitectureQualityTest_4
 *
 * <p>Pins the dependency direction between the low-level adapter packages of
 * {@code com.codemonk.common} - {@code cache}, {@code service} and
 * {@code exception} - so that infrastructure concerns stay decoupled from one
 * another and from the constant and error-handling layers, plus one
 * encapsulation rule that keeps interfaces from carrying state.
 */
public class ArchitectureQualityTest_4 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("DTOs should not depend on the service package")
    void dtosShouldNotDependOnServicePackage() {
        noClasses()
                .that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAPackage("..service..")
                .because("response payloads must not be shaped by the services that happen to produce them")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Cache package should not depend on the exception package")
    void cachePackageShouldNotDependOnExceptionPackage() {
        noClasses()
                .that().resideInAPackage("..cache..")
                .should().dependOnClassesThat()
                .resideInAPackage("..exception..")
                .because("cache adapters report failure through their own return types, not domain exceptions")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Cache package should not depend on the constant package")
    void cachePackageShouldNotDependOnConstantPackage() {
        noClasses()
                .that().resideInAPackage("..cache..")
                .should().dependOnClassesThat()
                .resideInAPackage("..constant..")
                .because("cache adapters are self-contained and must not reach into unrelated shared constants")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Service package should not depend on the cache package")
    void servicePackageShouldNotDependOnCachePackage() {
        noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAPackage("..cache..")
                .because("domain services must remain usable whether or not a cache is wired in")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception package should not depend on the cache package")
    void exceptionPackageShouldNotDependOnCachePackage() {
        noClasses()
                .that().resideInAPackage("..exception..")
                .should().dependOnClassesThat()
                .resideInAPackage("..cache..")
                .because("error handling must not be coupled to how results happen to be cached")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Interfaces should not declare fields")
    void interfacesShouldNotDeclareFields() {
        noFields()
                .should().beDeclaredInClassesThat().areInterfaces()
                .because("interface constants are a well-known anti-pattern that leaks implementation detail into a contract")
                .check(importedClasses);
    }
}
