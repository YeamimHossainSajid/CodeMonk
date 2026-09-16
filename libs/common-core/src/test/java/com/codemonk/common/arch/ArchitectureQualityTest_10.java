package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.codemonk.common.exception.DomainException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ArchitectureQualityTest_10
 *
 * <p>Covers how objects in {@code com.codemonk.common} are constructed and what
 * state they are allowed to hold: constructor contracts, field finality, and the
 * absence of mutable or global state in a library shared by every service.
 *
 * <p>The two constructor rules are expressed as custom {@link ArchCondition}
 * implementations, since neither is reachable through the fluent DSL.
 */
public class ArchitectureQualityTest_10 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private static final ArchCondition<JavaClass> DECLARE_EXACTLY_ONE_CONSTRUCTOR =
            new ArchCondition<>("declare exactly one constructor") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    int declared = item.getConstructors().size();
                    events.add(new SimpleConditionEvent(item, declared == 1,
                            String.format("%s declares %d constructors", item.getName(), declared)));
                }
            };

    private static final ArchCondition<JavaClass> DECLARE_A_MESSAGE_CONSTRUCTOR =
            new ArchCondition<>("declare a constructor taking a single String message") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean satisfied = item.getConstructors().stream()
                            .anyMatch(constructor -> constructor.getRawParameterTypes().size() == 1
                                    && constructor.getRawParameterTypes().get(0).isEquivalentTo(String.class));
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s %s a (String) constructor",
                                    item.getName(), satisfied ? "declares" : "does not declare")));
                }
            };

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Spring beans should declare exactly one constructor")
    void springBeansShouldDeclareExactlyOneConstructor() {
        classes()
                .that().areAnnotatedWith(Service.class)
                .or().areAnnotatedWith(Component.class)
                .or().areAnnotatedWith(RestControllerAdvice.class)
                .should(DECLARE_EXACTLY_ONE_CONSTRUCTOR)
                .because("a single constructor keeps injection unambiguous without @Autowired")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Fields of Spring beans should be final")
    void fieldsOfSpringBeansShouldBeFinal() {
        fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Component.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(RestControllerAdvice.class)
                .should().beFinal()
                .because("a collaborator injected once should never be reassigned afterwards")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain exceptions should offer a single-String message constructor")
    void domainExceptionsShouldOfferAMessageConstructor() {
        classes()
                .that().areAssignableTo(DomainException.class)
                .should(DECLARE_A_MESSAGE_CONSTRUCTOR)
                .because("every caller must be able to raise a failure with just a message")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain exceptions should be public")
    void domainExceptionsShouldBePublic() {
        classes()
                .that().areAssignableTo(DomainException.class)
                .should().bePublic()
                .because("consuming services must be able to throw and catch them")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Static fields should be final")
    void staticFieldsShouldBeFinal() {
        fields()
                .that().haveModifier(JavaModifier.STATIC)
                .should().beFinal()
                .because("mutable static state is shared by every consumer of the library")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Methods should not be synchronized")
    void methodsShouldNotBeSynchronized() {
        noMethods()
                .should().haveModifier(JavaModifier.SYNCHRONIZED)
                .because("locking in a shared library imposes contention on every caller")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Classes should not manage their own threads")
    void classesShouldNotManageTheirOwnThreads() {
        noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.lang.Thread")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Timer")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.concurrent.Executors")
                .because("thread lifecycles belong to the hosting service, not to the library")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Classes should never terminate the JVM")
    void classesShouldNeverTerminateTheJvm() {
        noClasses()
                .should().callMethod(System.class, "exit", int.class)
                .because("a library must never take down the service that embeds it")
                .check(importedClasses);
    }
}
