# ADR-002: Java and Spring Boot Version Strategy

* **Status**: Accepted
* **Date**: 2026-08-30

---

## 📌 Context & Problem Statement

As CodeMonk grows as an open-source enterprise AI platform, questions naturally arise regarding our technology stack baseline—specifically:
1. **Why are we currently on Java 21 and Spring Boot 3.3.0?**
2. **Why are we not immediately adopting non-LTS Java releases or unaligned cutting-edge Spring Boot versions?**
3. **What are the pros, cons, and compatibility risks associated with version updates in a microservices ecosystem?**

This document serves as the formal decision record and technical guide detailing the rationale behind CodeMonk's Java and Spring Boot version selection.

---

## 🎯 Architectural Rationale

### 1. Java Version Selection: Java 21 (Long-Term Support - LTS)

CodeMonk mandates **Java 21** (`<java.version>21</java.version>` in root `pom.xml`, enforced by `maven-enforcer-plugin`).

#### Key Reasons for Java 21 LTS:
* **Long-Term Enterprise Support (LTS)**: Java 21 is a premier LTS release supported through at least 2031 by major JDK vendors (Eclipse Temurin, Amazon Corretto, Oracle, Azul). In contrast, non-LTS feature releases (Java 22, Java 23, Java 24) reach End-Of-Life (EOL) after only 6 months, forcing unsustainable upgrade cycles on production infrastructure.
* **Production-Ready Virtual Threads (Project Loom)**: Java 21 natively introduces Virtual Threads (`java.lang.Thread.ofVirtual()`). In Spring Boot 3.3, setting `spring.threads.virtual.enabled=true` allows high-concurrency non-blocking I/O across our microservices (API Gateway, Repository Service, Search Service) without requiring complex reactive codebases or experimental compiler flags.
* **Finalized Language Innovations**: Java 21 includes finalized Record Patterns, Pattern Matching for `switch`, Sequenced Collections (`SequencedCollection`, `SequencedSet`, `SequencedMap`), enabling clean, type-safe data modeling for code AST nodes and vector schemas.
* **Container & APM Ecosystem Maturity**: Docker base images (e.g., `eclipse-temurin:21-jre-alpine`), CI/CD runners, OpenTelemetry agents, and bytecode tools (ByteBuddy, Lombok, Tree-Sitter Java bindings) are battle-tested against Java 21. Non-LTS releases frequently break build tools with `Unsupported class file major version` errors.

---

### 2. Spring Framework Baseline: Spring Boot 3.3.0

CodeMonk standardizes on **Spring Boot 3.3.0** with Jakarta EE 10 baseline.

#### Key Reasons for Spring Boot 3.3.0:
* **Spring Ecosystem Bill of Materials (BOM) Alignment**:
  * Microservices architecture requires synchronized dependency versions across service discovery (`discovery-server`), centralized config (`config-server`), and API routing (`api-gateway`).
  * Spring Boot 3.3.0 aligns directly with **Spring Cloud 2023.0.1 (Leyton)** and **Spring AI 1.0.0-M1**.
  * Arbitrarily upgrading Spring Boot outside of aligned Spring Cloud Release Trains leads to classpath conflicts, broken auto-configurations, and Eureka/Config Server incompatibilities.
* **Spring AI Integration Maturity**:
  * Spring AI (`1.0.0-M1`) relies on Spring Boot 3.3 abstractions for vector store integrations (pgvector), structured LLM prompt templates, and AI tool orchestration.
* **Jakarta EE 10 Standard**:
  * Standardized on `jakarta.*` packages for JPA/Hibernate 6.x, Spring Web, and Validation, eliminating legacy `javax.*` technical debt.

---

## ⚖️ Detailed Pros & Cons Matrix

| Dimension | Standard Baseline (Java 21 LTS + Spring Boot 3.3.x) | Bleeding-Edge Non-LTS / Cutting-Edge Versions |
| :--- | :--- | :--- |
| **Production Stability** | **High**: Tested vendor JDK binaries, stable patch releases, zero unexpected API churn. | **Low**: Risk of regression, rapid EOL (6 months), preview feature deprecation. |
| **Spring Cloud & AI BOM Matching** | **Seamless**: Validated compatibility between `spring-boot 3.3.0`, `spring-cloud 2023.0.1`, and `spring-ai 1.0.0-M1`. | **High Risk**: Unaligned BOMs cause classpath collisions, broken bean bindings, and gateway failures. |
| **Tooling & Build Pipeline Support** | **Full**: Works out-of-the-box with ArchUnit, SonarQube, Maven Enforcer, Lombok, and Tree-sitter parsers. | **Partial**: Maven plugins and byte-code generators often fail on unverified JDK major versions. |
| **Modern Developer Experience** | **Excellent**: Virtual Threads, Pattern Matching, Record DTOs, Spring Boot Observation API. | **Experimental**: Preview features require `--enable-preview` flags in compiler and JVM runtime. |
| **Maintenance & Security** | **Predictable**: Regular security patches (CVE fixes) via patch updates (e.g., 3.3.x). | **High Overhead**: Constant migration efforts every 6 months to avoid unsupported JDK builds. |

---

## 🛡️ Compatibility & Risks of Unplanned Upgrades

1. **Spring Cloud Gateway & Eureka Breakage**: Spring Cloud components are tightly coupled to specific Spring Boot release trains. Upgrading Spring Boot independently risks breaking circuit breakers, gateway filters, and service discovery registration.
2. **Spring AI Milestone Sensitivity**: Spring AI is actively evolving. Its auto-configurations, vector store connectors (`pgvector`), and agentic workflow tools are benchmarked against specific Spring Boot 3.x releases.
3. **Bytecode Instrumentation Failures**: Libraries like Lombok, ByteBuddy, and native C++ JNI wrappers (used in code parsing) require bytecode support from the underlying JDK. Non-LTS releases frequently trigger runtime initialization errors in CI/CD environments.

---

## 🚀 Upgrade & Governance Strategy

To ensure CodeMonk remains modern while maintaining rock-solid stability:

1. **Patch & Minor Updates**: We adopt patch releases within the Spring Boot 3.3 line (e.g., `3.3.x`) and Spring Cloud `2023.0.x` as security patches are released.
2. **Spring AI Progression**: We will bump `spring-ai` versions as new milestones (`M2`, `RC1`, `GA`) are released, verifying compatibility against our test suite (`mvn clean test`).
3. **Java LTS Tracking**: Java 21 remains our baseline JDK. Future major JDK upgrades will be considered upon the release and ecosystem adoption of the next LTS version (Java 25 LTS).
4. **Automated Enforcement**: Version boundaries are strictly enforced in root `pom.xml` via `maven-enforcer-plugin`:
   ```xml
   <requireJavaVersion>
       <version>[21,)</version>
   </requireJavaVersion>
   <requireMavenVersion>
       <version>[3.8,)</version>
   </requireMavenVersion>
   ```

---

## 🔗 Related References
* [ADR-001: Microservices Baseline Architecture](file:///Users/sajid/Documents/CodeMonk/docs/architecture/ADR-001-microservices-architecture.md)
* [CodeMonk Root POM](file:///Users/sajid/Documents/CodeMonk/pom.xml)
