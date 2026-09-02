package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ArchitectureQualityTest_15
 */
public class ArchitectureQualityTest_15 {

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.codemonk.common");
    }

    @Test
    @DisplayName("Exception classes should end with Exception suffix")
    void exceptionClassesShouldHaveExceptionSuffix() {
        classes()
                .that().resideInAPackage("..exception..")
                .and().areAssignableTo(Throwable.class)
                .should().haveSimpleNameEndingWith("Exception")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Services should not depend on GlobalExceptionHandler")
    void servicesShouldNotDependOnGlobalExceptionHandler() {
        classes()
                .that().resideInAPackage("..service..")
                .should().onlyDependOnClassesThat()
                .resideOutsideOfPackages("..exception.GlobalExceptionHandler..")
                .check(importedClasses);
    }
}
