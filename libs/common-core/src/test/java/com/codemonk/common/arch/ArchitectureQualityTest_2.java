package com.codemonk.common.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit rules verifying filter conventions, constant utility structures,
 * and service layering isolation.
 */
class ArchitectureQualityTest_2 {

    private static final String BASE_PACKAGE = "com.codemonk.common";
    private static final String FILTER_PACKAGE = BASE_PACKAGE + ".filter";
    private static final String CONSTANT_PACKAGE = BASE_PACKAGE + ".constant";
    private static final String SERVICE_PACKAGE = BASE_PACKAGE + ".service";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = ArchTestImports.importMainClasses(BASE_PACKAGE);
    }

    /**
     * Classes residing in the filter package must implement jakarta.servlet.Filter.
     */
    @Test
    void filterClassesShouldImplementFilter() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(FILTER_PACKAGE)
                .and()
                .areNotInterfaces()
                .should()
                .beAssignableTo(Filter.class);

        rule.check(classes);
    }

    /**
     * Filter classes must be Spring components.
     */
    @Test
    void filterClassesShouldBeComponents() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(FILTER_PACKAGE)
                .and()
                .areNotInterfaces()
                .should()
                .beAnnotatedWith(Component.class);

        rule.check(classes);
    }

    /**
     * Classes residing in the constant package should end with Constants.
     */
    @Test
    void constantClassesShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(CONSTANT_PACKAGE)
                .should()
                .haveSimpleNameEndingWith("Constants");

        rule.check(classes);
    }

    /**
     * Service layer classes must not depend on filter classes.
     */
    @Test
    void servicesShouldNotDependOnFilters() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(SERVICE_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(FILTER_PACKAGE);

        rule.check(classes);
    }
}
