package com.codemonk.common.arch;

import com.codemonk.common.dto.ErrorResponse;
import com.codemonk.common.dto.ValidationErrorDetail;
import com.codemonk.common.exception.DomainException;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchitectureQualityTest_6
 *
 * <p>Covers the failure path of {@code com.codemonk.common}: how a failure is
 * raised, where it may be caught, and how the global advice turns it into an
 * HTTP response. The sibling suites fix where exceptions live and what they are
 * named; this class checks that the path from {@code throw} to response body
 * stays complete and unambiguous.
 *
 * <p>Most rules here need a custom {@link ArchCondition}, since the fluent DSL
 * cannot inspect catch blocks, annotation values or generic return types.
 */
public class ArchitectureQualityTest_6 {

    private static final String ROOT_PACKAGE = "com.codemonk.common";

    private static final Set<String> TOO_BROAD_TO_CATCH = Set.of(
            Throwable.class.getName(), Error.class.getName(), Exception.class.getName());

    private static final ArchCondition<JavaClass> EXTEND_DOMAIN_EXCEPTION_DIRECTLY =
            new ArchCondition<>("extend DomainException directly") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean satisfied = item.getRawSuperclass()
                            .map(superclass -> superclass.isEquivalentTo(DomainException.class))
                            .orElse(false);
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s extends %s", item.getName(),
                                    item.getRawSuperclass().map(JavaClass::getName).orElse("nothing"))));
                }
            };

    private static final ArchCondition<JavaClass> NOT_CATCH_OVERLY_BROAD_THROWABLES =
            new ArchCondition<>("not catch Throwable, Error or Exception") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    for (JavaCodeUnit codeUnit : item.getCodeUnits()) {
                        for (TryCatchBlock block : codeUnit.getTryCatchBlocks()) {
                            for (JavaClass caught : block.getCaughtThrowables()) {
                                if (TOO_BROAD_TO_CATCH.contains(caught.getName())) {
                                    events.add(SimpleConditionEvent.violated(block,
                                            String.format("%s catches %s in %s",
                                                    codeUnit.getFullName(), caught.getSimpleName(),
                                                    block.getSourceCodeLocation())));
                                }
                            }
                        }
                    }
                }
            };

    private static final ArchCondition<JavaMethod> DECLARE_NO_THROWS_CLAUSE =
            new ArchCondition<>("declare no throws clause") {
                @Override
                public void check(JavaMethod item, ConditionEvents events) {
                    boolean satisfied = item.getThrowsClause().isEmpty();
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s declares throws %s",
                                    item.getFullName(), item.getThrowsClause().getTypes())));
                }
            };

    private static final ArchCondition<JavaMethod> ACCEPT_THE_HTTP_REQUEST =
            new ArchCondition<>("accept the HttpServletRequest") {
                @Override
                public void check(JavaMethod item, ConditionEvents events) {
                    boolean satisfied = item.getRawParameterTypes().stream()
                            .anyMatch(type -> type.isEquivalentTo(HttpServletRequest.class));
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s does not take an HttpServletRequest", item.getFullName())));
                }
            };

    private static final ArchCondition<JavaMethod> RESPOND_WITH_AN_ERROR_RESPONSE_BODY =
            new ArchCondition<>("return ResponseEntity<ErrorResponse>") {
                @Override
                public void check(JavaMethod item, ConditionEvents events) {
                    boolean satisfied = item.getReturnType() instanceof JavaParameterizedType parameterized
                            && item.getRawReturnType().isEquivalentTo(ResponseEntity.class)
                            && parameterized.getActualTypeArguments().size() == 1
                            && parameterized.getActualTypeArguments().get(0).toErasure()
                                    .isEquivalentTo(ErrorResponse.class);
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s returns %s", item.getFullName(), item.getReturnType().getName())));
                }
            };

    private static final ArchCondition<JavaClass> MAP_EACH_EXCEPTION_TYPE_EXACTLY_ONCE =
            new ArchCondition<>("name each handled exception type explicitly and in only one handler") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    Map<String, String> handlerByType = new HashMap<>();
                    for (JavaMethod method : item.getMethods()) {
                        if (!method.isAnnotatedWith(ExceptionHandler.class)) {
                            continue;
                        }
                        List<JavaClass> handled = handledTypes(method);
                        if (handled.isEmpty()) {
                            events.add(SimpleConditionEvent.violated(method, String.format(
                                    "%s does not name the exception types it handles", method.getFullName())));
                        }
                        for (JavaClass type : handled) {
                            String previous = handlerByType.putIfAbsent(type.getName(), method.getFullName());
                            if (previous != null) {
                                events.add(SimpleConditionEvent.violated(method, String.format(
                                        "%s is handled by both %s and %s",
                                        type.getName(), previous, method.getFullName())));
                            }
                        }
                    }
                }
            };

    private static final ArchCondition<JavaClass> DECLARE_A_CATCH_ALL_HANDLER =
            new ArchCondition<>("declare a handler for Exception") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean satisfied = item.getMethods().stream()
                            .filter(method -> method.isAnnotatedWith(ExceptionHandler.class))
                            .flatMap(method -> handledTypes(method).stream())
                            .anyMatch(type -> type.isEquivalentTo(Exception.class));
                    events.add(new SimpleConditionEvent(item, satisfied,
                            String.format("%s has no @ExceptionHandler(Exception.class)", item.getName())));
                }
            };

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = ArchTestImports.importMainClasses(ROOT_PACKAGE);
    }

    private static List<JavaClass> handledTypes(JavaMethod handler) {
        Object value = handler.getAnnotationOfType(ExceptionHandler.class.getName())
                .get("value")
                .orElse(new JavaClass[0]);
        return Stream.of((JavaClass[]) value).toList();
    }

    @Test
    @DisplayName("Domain exceptions should extend DomainException directly")
    void domainExceptionsShouldExtendDomainExceptionDirectly() {
        classes()
                .that().areAssignableTo(DomainException.class)
                .and().doNotHaveFullyQualifiedName(DomainException.class.getName())
                .should(EXTEND_DOMAIN_EXCEPTION_DIRECTLY)
                .because("a flat hierarchy keeps each failure's HTTP mapping readable in one place, "
                        + "without handler precedence between parent and child types")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Code should not catch Throwable, Error or Exception")
    void codeShouldNotCatchOverlyBroadThrowables() {
        classes()
                .should(NOT_CATCH_OVERLY_BROAD_THROWABLES)
                .because("a broad catch swallows failures the global advice is meant to map, "
                        + "and Errors such as OutOfMemoryError must reach the host service")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Public methods should not declare a throws clause")
    void publicMethodsShouldNotDeclareAThrowsClause() {
        methods()
                .that().arePublic()
                .should(DECLARE_NO_THROWS_CLAUSE)
                .because("the library reports failures through unchecked DomainExceptions, "
                        + "so callers never have to declare or wrap its failures")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception handlers should accept the HttpServletRequest")
    void exceptionHandlersShouldAcceptTheHttpRequest() {
        methods()
                .that().areAnnotatedWith(ExceptionHandler.class)
                .should(ACCEPT_THE_HTTP_REQUEST)
                .because("every error payload carries the request path and a trace id, "
                        + "and both are read from the request")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception handlers should respond with an ErrorResponse body")
    void exceptionHandlersShouldRespondWithAnErrorResponseBody() {
        methods()
                .that().areAnnotatedWith(ExceptionHandler.class)
                .should(RESPOND_WITH_AN_ERROR_RESPONSE_BODY)
                .because("clients parse every failure with one schema, whatever the status code")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception advice should map each exception type in exactly one handler")
    void exceptionAdviceShouldMapEachExceptionTypeOnce() {
        classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .should(MAP_EACH_EXCEPTION_TYPE_EXACTLY_ONCE)
                .because("Spring refuses to start when two handlers in one advice claim the same type, "
                        + "and an inferred mapping hides which types the advice covers")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception advice should declare a catch-all handler for Exception")
    void exceptionAdviceShouldDeclareACatchAllHandler() {
        classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .should(DECLARE_A_CATCH_ALL_HANDLER)
                .because("an unmapped failure must still produce an ErrorResponse with a trace id, "
                        + "never Spring's default error page")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Exception advice should run at the lowest precedence")
    void exceptionAdviceShouldRunAtTheLowestPrecedence() {
        classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .should().beAnnotatedWith(describe("@Order(Ordered.LOWEST_PRECEDENCE)",
                        (JavaAnnotation<?> annotation) -> annotation.getRawType().isEquivalentTo(Order.class)
                                && annotation.get("value")
                                        .map(value -> value.equals(Ordered.LOWEST_PRECEDENCE))
                                        .orElse(true)))
                .because("the library's advice is a fallback, so any advice a consuming service "
                        + "registers must take precedence over it")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Only the exception package should build error payloads")
    void onlyTheExceptionPackageShouldBuildErrorPayloads() {
        noClasses()
                .that().resideOutsideOfPackages("..exception..", "..dto..")
                .should().dependOnClassesThat().belongToAnyOf(ErrorResponse.class, ValidationErrorDetail.class)
                .because("code outside the web edge should throw a DomainException and let the "
                        + "advice render it, rather than build an error body of its own")
                .check(importedClasses);
    }
}
