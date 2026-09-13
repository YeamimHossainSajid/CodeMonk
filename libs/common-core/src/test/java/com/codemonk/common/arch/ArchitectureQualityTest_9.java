package com.codemonk.common.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.codemonk.common.exception.DomainException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * ArchitectureQualityTest_9
 *
 * <p>Pins down where specific kinds of classes are allowed to live and what
 * they must carry to be found there: domain exceptions, JPA entities, cache
 * beans, Jackson-backed DTOs and Kafka deserializers each get a location or
 * annotation rule, alongside two encapsulation rules that keep JSON wiring and
 * ORM dependencies from leaking across {@code com.codemonk.common}.
 */
public class ArchitectureQualityTest_9 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("Domain exceptions should reside in the exception package")
    void domainExceptionsShouldResideInTheExceptionPackage() {
        classes()
                .that().areAssignableTo(DomainException.class)
                .should().resideInAPackage("..exception..")
                .because("every failure type must live alongside the advice that maps it")
                .check(importedClasses);
    }

    @Test
    @DisplayName("JPA entities should reside in the service package")
    void jpaEntitiesShouldResideInTheServicePackage() {
        classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..service..")
                .because("persistence-backed entities belong with the services that own their lifecycle")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Cache package classes should be Spring-managed beans")
    void cachePackageClassesShouldBeSpringManagedBeans() {
        classes()
                .that().resideInAPackage("..cache..")
                .should().beAnnotatedWith(Component.class)
                .orShould().beAnnotatedWith(Service.class)
                .because("cache adapters must be discoverable and injectable as Spring beans")
                .check(importedClasses);
    }

    @Test
    @DisplayName("DTOs should be annotated to omit null fields from serialized responses")
    void dtosShouldOmitNullFieldsWhenSerialized() {
        classes()
                .that().resideInAPackage("..dto..")
                .should().beAnnotatedWith(JsonInclude.class)
                .because("omitting null fields keeps API error payloads compact and predictable")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Kafka deserializers should not depend on the dto package")
    void kafkaDeserializersShouldNotDependOnDtoPackage() {
        noClasses()
                .that().implement(Deserializer.class)
                .should().dependOnClassesThat()
                .resideInAPackage("..dto..")
                .because("wire-format decoding must not couple to the shape of an API response")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Only the constant package should construct an ObjectMapper")
    void onlyTheConstantPackageShouldConstructAnObjectMapper() {
        noClasses()
                .that().resideOutsideOfPackage("..constant..")
                .should().callConstructor(ObjectMapper.class)
                .because("JsonConstants centralizes ObjectMapper configuration so every caller shares it")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Production code should not depend on Hibernate directly")
    void shouldNotDependOnHibernateDirectly() {
        noClasses()
                .should().dependOnClassesThat()
                .resideInAPackage("org.hibernate..")
                .because("persistence code must depend on the jakarta.persistence API, not a specific ORM")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Fields declared in the constant package should be public")
    void fieldsInTheConstantPackageShouldBePublic() {
        fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..constant..")
                .should().bePublic()
                .because("constant holders are a namespace of public values, not encapsulated state")
                .check(importedClasses);
    }
}
