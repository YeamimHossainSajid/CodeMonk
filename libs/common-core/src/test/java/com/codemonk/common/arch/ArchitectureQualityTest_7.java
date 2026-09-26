package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * ArchitectureQualityTest_7
 *
 * <p>Checks that a type's name and shape tell the truth about what it is, and
 * that state and environment access stay with the class that owns them. A
 * {@code *Service} is a contract or a bean, a {@code *Constants} class is a
 * constant holder, an interface is a pure contract, and an instance field is
 * read and written by its declaring class alone.
 *
 * <p>The sibling classes mostly police dependencies <em>between packages</em>.
 * This one polices the boundary <em>around each class</em>, plus a few
 * transport and configuration APIs that no other rule confines yet.
 */
public class ArchitectureQualityTest_7 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private static final ArchCondition<JavaField> ONLY_BE_ACCESSED_BY_THEIR_DECLARING_CLASS =
            new ArchCondition<>("only be accessed by their declaring class") {
                @Override
                public void check(JavaField item, ConditionEvents events) {
                    item.getAccessesToSelf().stream()
                            .filter(access -> !access.getOriginOwner().equals(item.getOwner()))
                            .forEach(access -> events.add(SimpleConditionEvent.violated(item,
                                    String.format("%s is accessed from %s",
                                            item.getFullName(), access.getOrigin().getFullName()))));
                }
            };

    private static final ArchCondition<JavaClass> DECLARE_NO_FIELDS =
            new ArchCondition<>("declare no fields") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    int declared = item.getFields().size();
                    events.add(new SimpleConditionEvent(item, declared == 0,
                            String.format("%s declares %d field(s)", item.getName(), declared)));
                }
            };

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = ArchTestImports.importMainClasses(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Classes named *Service should be interfaces or @Service beans")
    void serviceNamedClassesShouldBeContractsOrBeans() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .should().beInterfaces()
                .orShould().beAnnotatedWith(Service.class)
                .because("a *Service name promises either a contract to implement or a bean to inject")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Constant holders should be named *Constants, and *Constants classes should be constant holders")
    void constantHoldersShouldMatchTheirNamingConvention() {
        classes()
                .that().resideInAPackage("..constant..")
                .should().haveSimpleNameEndingWith("Constants")
                .check(importedClasses);

        classes()
                .that().haveSimpleNameEndingWith("Constants")
                .should().resideInAPackage("..constant..")
                .because("a caller looking for a shared value should only ever need to look in one package")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Instance fields should only be accessed by their declaring class")
    void instanceFieldsShouldOnlyBeAccessedByTheirDeclaringClass() {
        fields()
                .that().areNotStatic()
                .should(ONLY_BE_ACCESSED_BY_THEIR_DECLARING_CLASS)
                .because("package-private state reached from a neighbour bypasses the owner's invariants")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Property injection with @Value should be confined to the config package")
    void valueInjectionShouldBeConfinedToTheConfigPackage() {
        fields()
                .that().areAnnotatedWith(Value.class)
                .should().beDeclaredInClassesThat().resideInAPackage("..config..")
                .because("adapters should receive settings through their constructor, not read property keys")
                .check(importedClasses);
    }

    @Test
    @DisplayName("The servlet API should be confined to the exception advice and filter packages")
    void servletApiShouldBeConfinedToTheWebEdge() {
        noClasses()
                .that().resideOutsideOfPackages("..exception..", "..filter..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.servlet..")
                .because("cache, service and config code must also run outside a servlet container")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Interfaces should be pure contracts without state or stereotypes")
    void interfacesShouldBePureContracts() {
        classes()
                .that().areInterfaces()
                .should(DECLARE_NO_FIELDS)
                .andShould().notBeAnnotatedWith(Component.class)
                .andShould().notBeAnnotatedWith(Service.class)
                .because("constants belong in the constant package, and Spring registers implementations, not interfaces")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Nested classes should be static")
    void nestedClassesShouldBeStatic() {
        classes()
                .that().areMemberClasses()
                .should().haveModifier(JavaModifier.STATIC)
                .because("an inner class keeps a hidden reference to its enclosing bean alive")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Fields should not be of type Optional")
    void fieldsShouldNotBeOptional() {
        noFields()
                .should().haveRawType(Optional.class)
                .because("Optional is a return type for absent results, not a nullable holder for state")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Production code should not use legacy synchronized collections")
    void shouldNotUseLegacySynchronizedCollections() {
        noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Vector")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Hashtable")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Stack")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.lang.StringBuffer")
                .because("per-call locking adds contention without making compound operations thread-safe")
                .check(importedClasses);
    }
}
