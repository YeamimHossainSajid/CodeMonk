package com.codemonk.common.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit rules enforcing naming conventions and layering boundaries
 * between the {@code dto} and {@code exception} packages of {@code com.codemonk.common}.
 */
class ArchitectureQualityTest_1 {

    private static final String BASE_PACKAGE = "com.codemonk.common";
    private static final String DTO_PACKAGE = BASE_PACKAGE + ".dto";
    private static final String EXCEPTION_PACKAGE = BASE_PACKAGE + ".exception";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = ArchTestImports.importMainClasses(BASE_PACKAGE);
    }

    /**
     * DTO classes must end with {@code Response}, {@code Dto}, {@code Request}, or {@code Detail}.
     */
    @Test
    void dtoClassesShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(DTO_PACKAGE)
                .and()
                .areNotInterfaces()
                .should()
                .haveSimpleNameEndingWith("Response")
                .orShould()
                .haveSimpleNameEndingWith("Dto")
                .orShould()
                .haveSimpleNameEndingWith("Request")
                .orShould()
                .haveSimpleNameEndingWith("Detail");

        rule.check(classes);
    }

    /**
     * Classes in the exception package must extend {@link Throwable},
     * except for the {@link RestControllerAdvice}-annotated handler.
     */
    @Test
    void exceptionClassesShouldExtendThrowable() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(EXCEPTION_PACKAGE)
                .and()
                .areNotAnnotatedWith(RestControllerAdvice.class)
                .should()
                .beAssignableTo(Throwable.class);

        rule.check(classes);
    }

    /**
     * Exception classes must not depend on DTO classes, keeping error types decoupled from the API layer.
     */
    @Test
    void exceptionClassesShouldNotDependOnDtoClasses() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(EXCEPTION_PACKAGE)
                .and()
                .areAssignableTo(Throwable.class)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(DTO_PACKAGE);

        rule.check(classes);
    }

    /**
     * DTO classes must be public since they form part of the API contract.
     */
    @Test
    void dtoClassesPublicAPI() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(DTO_PACKAGE)
                .should()
                .bePublic();

        rule.check(classes);
    }
}
