# ADR-001: Microservices Baseline Architecture

* **Status**: Accepted
* **Date**: 2026-08-26

## Context

CodeMonk is an open-source AI platform designed to analyze, index, and reason over multi-service enterprise repositories. The codebase needs to accommodate independent contributions from developers specializing in backend Java/Spring, streaming pipelines, graph modeling, vector retrieval, and AI orchestration.

## Decision

We adopt a decoupled Spring Boot 3.x multi-module microservices architecture:

1. **Service Boundaries**:
   - `api-gateway`: Single external entry point routing requests to internal services via Eureka discovery.
   - `discovery-server`: Service registration via Spring Cloud Netflix Eureka.
   - `config-server`: Centralized externalized configuration management via Spring Cloud Config.
   - `repository-service`: Manages repository lifecycles and metadata.
   - `code-analysis-service`: Performs AST parsing and code relation extraction via Kafka event streams.
   - `knowledge-service`: Constructs and queries code relationship graphs.
   - `search-service`: Handles hybrid vector + lexical code snippet indexing and retrieval.
   - `ai-service`: Manages Spring AI integrations, LLM prompts, and agentic workflows.
   - `documentation-service`: Generates architectural documentation and repository summaries.

2. **Data & Persistence Ownership**:
   - Each service maintains complete ownership over its persistence store.
   - No shared entity classes or cross-database transactions are permitted across microservices.

3. **Communication**:
   - Asynchronous event streaming via Apache Kafka for heavy processing tasks (code indexing, AST extraction).
   - Synchronous REST via Spring Cloud Gateway for API queries.

## Consequences

- Contributors can focus on a single service in `services/<service-name>` without needing deep knowledge of other modules.
- Independent scalability and technology choices per service (e.g., pgvector in search-service, Neo4j in knowledge-service).
