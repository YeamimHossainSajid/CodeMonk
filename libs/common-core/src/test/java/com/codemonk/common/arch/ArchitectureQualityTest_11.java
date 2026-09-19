package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ArchitectureQualityTest_11
 *
 * <p>Keeps third-party technologies confined to the package that owns them and
 * pins the member-level conventions (constructors, fields, handler methods)
 * that the {@code com.codemonk.common} library relies on.
 */
public class ArchitectureQualityTest_11 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Redis should be confined to the cache package")
    void redisShouldBeConfinedToTheCachePackage() {
        noClasses()
                .that().resideOutsideOfPackage("..cache..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.data.redis..")
                .because("callers should depend on RedisCacheService, not on RedisTemplate")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Kafka should be confined to the service package")
    void kafkaShouldBeConfinedToTheServicePackage() {
        noClasses()
                .that().resideOutsideOfPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.apache.kafka..")
                .because("messaging types belong to the serde layer, not to shared contracts")
                .check(importedClasses);
    }

    @Test
    @DisplayName("JPA should not leak into shared contract packages")
    void jpaShouldNotLeakIntoSharedContractPackages() {
        noClasses()
                .that().resideOutsideOfPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.persistence..")
                .because("cache, dto, exception and constant types are shared across services "
                        + "and must not carry a persistence mapping")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Date and time handling should use java.time")
    void dateHandlingShouldUseJavaTime() {
        noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Date")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Calendar")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("java.text.SimpleDateFormat")
                .because("the legacy date API is mutable and not thread-safe")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception handler methods should live in a @RestControllerAdvice class")
    void exceptionHandlersShouldLiveInRestControllerAdvice() {
        methods()
                .that().areAnnotatedWith(ExceptionHandler.class)
                .should().beDeclaredInClassesThat()
                .areAnnotatedWith(RestControllerAdvice.class)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception handler methods should be public and return ResponseEntity")
    void exceptionHandlersShouldReturnResponseEntity() {
        methods()
                .that().areAnnotatedWith(ExceptionHandler.class)
                .should().bePublic()
                .andShould().haveRawReturnType(ResponseEntity.class)
                .because("every handled failure must reach the client as a status plus a body")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Constant holders should hide their constructor")
    void constantHoldersShouldHideTheirConstructor() {
        classes()
                .that().resideInAPackage("..constant..")
                .should().haveOnlyPrivateConstructors()
                .because("constant holders are namespaces, never instantiated")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Instance fields should never be public")
    void instanceFieldsShouldNeverBePublic() {
        noFields()
                .that().doNotHaveModifier(JavaModifier.STATIC)
                .should().bePublic()
                .because("mutable state must stay behind the declaring type's own API")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Kafka deserializers should carry the Deserializer suffix")
    void kafkaDeserializersShouldCarryTheDeserializerSuffix() {
        classes()
                .that().implement(Deserializer.class)
                .should().haveSimpleNameEndingWith("Deserializer")
                .andShould().resideInAPackage("..service..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Every class should reside in one of the approved packages")
    void everyClassShouldResideInAnApprovedPackage() {
        classes()
                .should().resideInAnyPackage(
                        ROOT_PACKAGE + ".cache..",
                        ROOT_PACKAGE + ".config..",
                        ROOT_PACKAGE + ".constant..",
                        ROOT_PACKAGE + ".dto..",
                        ROOT_PACKAGE + ".exception..",
                        ROOT_PACKAGE + ".filter..",
                        ROOT_PACKAGE + ".service..")
                .because("a new top-level package is an architectural decision, not an accident")
                .check(importedClasses);
    }
}
