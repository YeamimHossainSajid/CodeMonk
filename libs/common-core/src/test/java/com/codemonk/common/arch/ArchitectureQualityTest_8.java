package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
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
 * ArchitectureQualityTest_8
 *
 * <p>Guards the published surface of {@code com.codemonk.common} rather than its
 * internal layering: which types and members a consuming service can actually
 * reach, what those members are allowed to hand back, and what the library may
 * reach for in the runtime it is embedded in.
 *
 * <p>Where {@code ArchitectureQualityTest_12} and {@code ArchUnitArchitectureTests}
 * pin the direction of dependencies between packages, and
 * {@code ArchitectureQualityTest_10} pins how objects are built, this class asks
 * a different question: is the API a downstream microservice compiles against
 * stable, self-describing and free of ambient environment access?
 */
public class ArchitectureQualityTest_8 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    /**
     * Factory methods are the library's supported construction path, so they must
     * return the type that declares them rather than a supertype or a raw value.
     */
    private static final ArchCondition<JavaClass> DECLARE_SELF_RETURNING_FACTORY_METHODS =
            new ArchCondition<>("declare only self-returning static factory methods") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    item.getMethods().stream()
                            .filter(method -> method.getModifiers().contains(JavaModifier.STATIC))
                            .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                            .forEach(method -> {
                                boolean satisfied = method.getRawReturnType().isEquivalentTo(
                                        item.reflect());
                                events.add(new SimpleConditionEvent(method, satisfied,
                                        String.format("%s returns %s instead of %s",
                                                method.getFullName(),
                                                method.getRawReturnType().getSimpleName(),
                                                item.getSimpleName())));
                            });
                }
            };

    private static final ArchCondition<JavaClass> DECLARE_NO_METHODS =
            new ArchCondition<>("declare no methods beyond their constructors") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    int declared = item.getMethods().size();
                    events.add(new SimpleConditionEvent(item, declared == 0,
                            String.format("%s declares %d method(s)", item.getName(), declared)));
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
    @DisplayName("Every top-level type in the library should be public")
    void everyTopLevelTypeShouldBePublic() {
        classes()
                .that().areTopLevelClasses()
                .should().bePublic()
                .because("a package-private type in a shared library is unreachable "
                        + "from the services that depend on it")
                .check(importedClasses);
    }

    @Test
    @DisplayName("DTO factory methods should return the DTO that declares them")
    void dtoFactoryMethodsShouldReturnTheirOwnType() {
        classes()
                .that().resideInAPackage("..dto..")
                .should(DECLARE_SELF_RETURNING_FACTORY_METHODS)
                .because("ErrorResponse.of(..) is the supported way to build a payload, "
                        + "so its return type is part of the contract")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Public methods should not expose framework types to callers")
    void publicMethodsShouldNotExposeFrameworkTypesToCallers() {
        noMethods()
                .that().arePublic()
                .and().areDeclaredInClassesThat().resideInAnyPackage("..cache..", "..dto..", "..constant..")
                .should().haveRawReturnType(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "a Spring or Jakarta type",
                                javaClass -> javaClass.getPackageName().startsWith("org.springframework")
                                        || javaClass.getPackageName().startsWith("jakarta")))
                .because("a caller must not be forced onto Spring's classpath to read a return value")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Cache adapters should hand back values, never the template that produced them")
    void cacheAdaptersShouldNotLeakTheirTemplate() {
        noMethods()
                .that().areDeclaredInClassesThat().resideInAPackage("..cache..")
                .and().arePublic()
                .should().haveRawReturnType(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "a Redis operations type",
                                javaClass -> javaClass.getPackageName().startsWith("org.springframework.data.redis")))
                .because("handing out the RedisTemplate would let callers bypass the "
                        + "failure handling the adapter exists to provide")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain exceptions should add no API beyond their constructors")
    void domainExceptionsShouldAddNoApiBeyondConstructors() {
        // Expressed as a condition on the class rather than through `noMethods()`:
        // ArchUnit models constructors separately from methods, so a `noMethods()`
        // selector over these types matches nothing at all and the rule fails as
        // vacuous instead of passing. Checking the declaring class lets the rule
        // assert the real invariant -- constructors only -- and keep asserting it
        // if someone later adds a method.
        classes()
                .that().areAssignableTo(DomainException.class)
                .should(DECLARE_NO_METHODS)
                .because("a failure type carries a message and a cause; behaviour on it "
                        + "would have to be re-implemented by every catch site")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Spring beans should not expose static mutators")
    void springBeansShouldNotExposeStaticMutators() {
        noMethods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Component.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(RestControllerAdvice.class)
                .should().haveModifier(JavaModifier.STATIC)
                .andShould().bePublic()
                .because("a public static entry point on a bean is reachable without "
                        + "injection and bypasses the container's lifecycle")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Public API methods should not be marked final")
    void publicApiMethodsShouldNotBeFinal() {
        noMethods()
                .that().arePublic()
                .and().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .should().haveModifier(JavaModifier.FINAL)
                .because("Spring proxies a bean by subclassing it, and a final method "
                        + "silently drops out of the proxy")
                .check(importedClasses);
    }

    @Test
    @DisplayName("The library should not read ambient environment or system state")
    void libraryShouldNotReadAmbientEnvironmentState() {
        noClasses()
                .should().callMethod(System.class, "getenv", String.class)
                .orShould().callMethod(System.class, "getenv")
                .orShould().callMethod(System.class, "getProperty", String.class)
                .orShould().callMethod(System.class, "setProperty", String.class, String.class)
                .because("configuration must arrive through Spring, so behaviour stays the "
                        + "same in every service that embeds the library")
                .check(importedClasses);
    }

    @Test
    @DisplayName("The library should not use reflection to reach around its own API")
    void libraryShouldNotUseReflection() {
        noClasses()
                .should().dependOnClassesThat()
                .resideInAPackage("java.lang.reflect..")
                .because("reflective access defeats the boundaries every other rule in "
                        + "this suite enforces at compile time")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Types crossing the cache boundary should be immutable records")
    void typesCrossingTheCacheBoundaryShouldBeImmutableRecords() {
        // The original form of this rule selected on Serializable and checked for a
        // serialVersionUID. Nothing in the library implements Serializable -- values
        // reach Redis through Jackson, not Java serialization -- so that selector
        // matched nothing and the rule failed as vacuous. Immutability of the DTOs
        // is the invariant that actually protects a cached payload's shape here.
        classes()
                .that().resideInAPackage("..dto..")
                .should().beRecords()
                .andShould().haveOnlyFinalFields()
                .because("a value cached under a key must not change shape after it has "
                        + "been written")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Methods returning a collection should return an interface type")
    void methodsReturningCollectionsShouldReturnInterfaceTypes() {
        methods()
                .that().arePublic()
                .and().haveRawReturnType(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "a concrete java.util collection",
                                javaClass -> javaClass.getPackageName().equals("java.util")
                                        && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT)
                                        && javaClass.isAssignableTo(java.util.Collection.class)))
                .should().beDeclaredInClassesThat().resideInAPackage("..nowhere..")
                .allowEmptyShould(true)
                .because("returning ArrayList rather than List freezes an implementation "
                        + "choice into the published signature")
                .check(importedClasses);
    }
}
