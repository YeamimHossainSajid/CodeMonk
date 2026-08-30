# 📁 CodeMonk Issue Implementation Directory & Architecture Guide

Welcome to the **CodeMonk Issue Implementation Directory**! This guide is designed to help contributors quickly identify **which microservice**, **package**, and **file path** their assigned issue belongs to, along with step-by-step instructions on how to implement and test their changes.

---

## 🏛️ Microservice Module Mapping

CodeMonk is built as a modular enterprise system. All source code is organized under `libs/` (shared core libraries) or `services/` (independent microservices):

```
CodeMonk/
├── libs/
│   └── common-core/                  # Shared models, DTOs, Redis/Kafka configs, base test setups
└── services/
    ├── api-gateway/                  # Spring Cloud Gateway (Routing, Rate Limiting, CORS)
    ├── config-server/                # Centralized Spring Cloud Config Server
    ├── discovery-server/             # Netflix Eureka Service Registry
    ├── repository-service/           # Git ingestion, repository validation, metadata extraction
    ├── code-analysis-service/        # AST parsing, Tree-sitter integration, structural extraction
    ├── knowledge-service/            # Neo4j graph entities, Cypher repos, graph relationships
    ├── search-service/               # Vector search (pgvector), hybrid RAG, search key caching
    ├── ai-service/                   # Spring AI multi-agent tools, LLM clients, prompt templates
    └── documentation-service/        # Architectural summary generator & documentation engine
```

---

## 🧭 Issue Categories & Target Package Matrix

| Issue Category | Microservice / Module | Target Directory | Package Path |
| :--- | :--- | :--- | :--- |
| **Architecture Quality Tests (`ArchitectureQualityTest_*`)** | `libs/common-core` | `libs/common-core/src/test/java/` | `com.codemonk.common.arch` |
| **Mock Service Clients (`Mock*ServiceClient`)** | `services/ai-service`, `search-service`, `knowledge-service` | `services/<service>/src/test/java/` | `com.codemonk.<service>.client` |
| **WireMock & Integration Tests (`WireMockTestSetup`, `AbstractBaseIntegrationTest`)** | `libs/common-core` | `libs/common-core/src/test/java/` | `com.codemonk.common.test` |
| **ArchUnit Layer Tests (`ArchUnit*`)** | `libs/common-core` | `libs/common-core/src/test/java/` | `com.codemonk.common.arch` |
| **Testcontainers Config (`TestcontainersConfig`)** | `libs/common-core` | `libs/common-core/src/test/java/` | `com.codemonk.common.config` |
| **Redis Caching (`RedisCacheService`, `CacheEvictionService`, `*CacheKeyGenerator`)** | `libs/common-core` | `libs/common-core/src/main/java/` | `com.codemonk.common.cache` |
| **Redis Unit Tests (`RedisCacheTest_*`)** | `libs/common-core` | `libs/common-core/src/test/java/` | `com.codemonk.common.cache` |
| **Kafka Event Handlers & DLQ (`DlqProducerService`, `KafkaEventTest_*`)** | `libs/common-core` | `libs/common-core/src/main/java/` / `src/test/java/` | `com.codemonk.common.event` |
| **Repository Service Features (`RepositoryValidationService`, etc.)** | `services/repository-service` | `services/repository-service/src/main/java/` | `com.codemonk.repository.service` |
| **API Gateway Configuration (`Configure Gateway`, etc.)** | `services/api-gateway` | `services/api-gateway/src/main/java/` | `com.codemonk.gateway.config` |
| **Eureka Discovery (`Configure Eureka`, etc.)** | `services/discovery-server` | `services/discovery-server/src/main/java/` | `com.codemonk.discovery.config` |

---

## 🛠️ Step-by-Step Implementation Guidelines by Category

### 1. 🧪 Architecture Quality Tests (`ArchitectureQualityTest_1..15`)
- **Directory**: `libs/common-core/src/test/java/com/codemonk/common/arch/`
- **Class Name**: `ArchitectureQualityTest_X.java`
- **Package**: `package com.codemonk.common.arch;`
- **Implementation**:
  1. Create class extending `AbstractBaseArchTest` (or using ArchUnit `@AnalyzeClasses(packages = "com.codemonk")`).
  2. Implement specific architecture rules (e.g. enforcing `@Repository` classes reside in `.repository` package).
  3. Verify with `./mvnw test -pl libs/common-core`.

---

### 2. ⚡ Redis Caching & Key Generators
- **Directory**: `libs/common-core/src/main/java/com/codemonk/common/cache/`
- **Package**: `package com.codemonk.common.cache;`
- **Implementation**:
  1. Create key generator class implementing `org.springframework.cache.interceptor.KeyGenerator`.
  2. Implement `generate(Object target, Method method, Object... params)` to return structured string key formatted as `codemonk:<domain>:<id>`.
  3. Annotate class with `@Component("<Name>CacheKeyGenerator")`.

---

### 3. 📨 Kafka Event Processing & DLQ Services
- **Directory**: `libs/common-core/src/main/java/com/codemonk/common/event/`
- **Package**: `package com.codemonk.common.event;`
- **Implementation**:
  1. Inject `KafkaTemplate<String, Object>` into service.
  2. Implement message serialization and dead-letter queue routing for failed message execution.
  3. Annotate class with `@Service`.

---

### 4. 🌐 API Gateway Routes & Filters
- **Directory**: `services/api-gateway/src/main/java/com/codemonk/gateway/config/`
- **Package**: `package com.codemonk.gateway.config;`
- **Implementation**:
  1. Define `@Configuration` class declaring `RouteLocator` bean via `RouteLocatorBuilder`.
  2. Configure path predicate, rewrite path filters, and circuit breaker fallback handlers.

---

## 🚀 How to Pick & Solve an Issue

1. **Find your issue number** on GitHub (e.g. `#387`).
2. **Check the issue body** for the exact Target File Path, Package Name, and Microservice.
3. Create a git branch: `git checkout -b feature/issue-XXX`.
4. Implement the class in the specified package location.
5. Run module unit tests: `./mvnw test -pl <target-module>`.
6. Push your branch and open a PR!
