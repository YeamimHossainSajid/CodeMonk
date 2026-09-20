package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.codemonk.common.exception.DomainException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ArchitectureQualityTest_3
 *
 * <p>Pins the contracts that each framework extension point in
 * {@code com.codemonk.common} has to honour in order to be picked up at
 * runtime: JPA entities, Kafka deserializers, servlet filters, Spring
 * configuration classes and the global exception advice. A handful of
 * dependency-hygiene rules round it off by keeping Spring Boot, reflection
 * and JDK-internal APIs out of the shared code paths.
 *
 * <p>The reflective-instantiation and identifier rules are expressed as
 * custom {@link ArchCondition} implementations, since neither is reachable
 * through the fluent DSL.
 */
public class ArchitectureQualityTest_3 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private static final ArchCondition<JavaClass> DECLARE_A_PUBLIC_NO_ARG_CONSTRUCTOR =
            new ArchCondition<>("declare a public no-arg constructor") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean satisfied = item.getConstructors().stream()
                            .anyMatch(constructor -> constructor.getRawParameterTypes().isEmpty()
                                    && constructor.getModifiers().contains(JavaModifier.PUBLIC));
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s %s a public no-arg constructor",
                                    item.getName(), satisfied ? "declares" : "does not declare")));
                }
            };

    private static final ArchCondition<JavaClass> DECLARE_AN_ID_FIELD =
            new ArchCondition<>("declare a field annotated with @Id") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean satisfied = item.getFields().stream()
                            .anyMatch(field -> field.isAnnotatedWith(Id.class));
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s %s a field annotated with @Id",
                                    item.getName(), satisfied ? "declares" : "does not declare")));
                }
            };

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = ArchTestImports.importMainClasses(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("JPA entities should declare a public no-arg constructor")
    void jpaEntitiesShouldDeclareAPublicNoArgConstructor() {
        classes()
                .that().areAnnotatedWith(Entity.class)
                .should(DECLARE_A_PUBLIC_NO_ARG_CONSTRUCTOR)
                .because("the persistence provider instantiates entities reflectively")
                .check(importedClasses);
    }

    @Test
    @DisplayName("JPA entities should declare an @Id field")
    void jpaEntitiesShouldDeclareAnIdField() {
        classes()
                .that().areAnnotatedWith(Entity.class)
                .should(DECLARE_AN_ID_FIELD)
                .because("an entity without an identifier cannot be managed by a persistence context")
                .check(importedClasses);
    }

    @Test
    @DisplayName("JPA entity fields should be private")
    void jpaEntityFieldsShouldBePrivate() {
        fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                .should().bePrivate()
                .because("entity state must only be reachable through accessors the provider can intercept")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Kafka deserializers should declare a public no-arg constructor")
    void kafkaDeserializersShouldDeclareAPublicNoArgConstructor() {
        classes()
                .that().implement(Deserializer.class)
                .should(DECLARE_A_PUBLIC_NO_ARG_CONSTRUCTOR)
                .because("Kafka instantiates deserializers by class name from consumer properties")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Servlet filters should extend OncePerRequestFilter and be Spring components")
    void servletFiltersShouldExtendOncePerRequestFilterAndBeComponents() {
        classes()
                .that().resideInAPackage("..filter..")
                .should().beAssignableTo(OncePerRequestFilter.class)
                .andShould().beAnnotatedWith(Component.class)
                .andShould().haveSimpleNameEndingWith("Filter")
                .because("a filter must run exactly once per request and be registered automatically")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Servlet filters should not depend on any other common-core package")
    void servletFiltersShouldNotDependOnOtherCommonCorePackages() {
        noClasses()
                .that().resideInAPackage("..filter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..cache..", "..config..", "..dto..", "..exception..", "..service..")
                .because("request filters are cross-cutting and must not pull in domain or adapter code")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Config package classes should be @Configuration and not be referenced elsewhere")
    void configPackageClassesShouldBeConfigurationsAndStandalone() {
        classes()
                .that().resideInAPackage("..config..")
                .should().beAnnotatedWith(Configuration.class)
                .andShould().onlyHaveDependentClassesThat().resideInAPackage("..config..")
                .because("configuration is wired by the container, never called by library code")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Spring Boot should be confined to the config package")
    void springBootShouldBeConfinedToTheConfigPackage() {
        noClasses()
                .that().resideOutsideOfPackage("..config..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.boot..")
                .because("shared adapters must work in any Spring context, not only a Boot application")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception advice should declare an explicit @Order")
    void exceptionAdviceShouldDeclareAnExplicitOrder() {
        classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .should().beAnnotatedWith(Order.class)
                .because("consuming services register their own advice and must be able to take precedence")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception handler methods should be named handle*")
    void exceptionHandlerMethodsShouldBeNamedHandle() {
        methods()
                .that().areAnnotatedWith(ExceptionHandler.class)
                .should().haveNameStartingWith("handle")
                .because("a uniform prefix keeps the mapping table of the advice readable at a glance")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain exceptions should not carry state")
    void domainExceptionsShouldNotCarryState() {
        noFields()
                .should().beDeclaredInClassesThat().areAssignableTo(DomainException.class)
                .because("failures are described by their message; extra state would leak into API payloads")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Production code should not use reflection or JDK-internal APIs")
    void shouldNotUseReflectionOrJdkInternalApis() {
        noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("java.lang.reflect..", "sun..", "jdk.internal..")
                .because("reflective and internal access breaks under the module system and AOT compilation")
                .check(importedClasses);
    }
}
