# Contributing to CodeMonk 🥋

Thank you for your interest in contributing to CodeMonk! CodeMonk is built by a community of developers passionate about code intelligence, distributed systems, and modern AI.

---

## 🎯 Architectural Principles

When contributing code to any microservice, please adhere to our core architecture principles:

1. **Service Boundaries**: Each microservice under `services/` is completely independent.
2. **Database Ownership**: One service owns its database (`ONE SERVICE OWNS ITS DATA`). Never create shared entity models or cross-database queries.
3. **No Domain Models in Interfaces**: Controllers in `interfaces/` must talk through application use cases or DTOs, not directly expose domain models.
4. **Package Layout**: Business microservices follow `com.codemonk.<service>` with `domain`, `application`, `infrastructure`, and `interfaces` layers.
5. **No Secrets in Code**: Environment variables must be used for sensitive configuration.

---

## 🚀 How to Contribute

1. **Pick an Issue**: Search GitHub Issues for labels like `good first issue`, `beginner`, or `help wanted`.
2. **Fork & Clone**: Fork the repository on GitHub and clone your fork locally.
3. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/short-description
   ```
4. **Build & Test Locally**:
   Ensure all microservice tests pass before opening a PR:
   ```bash
   mvn clean test
   ```
5. **Open a Pull Request**: Submit your PR with a clear description of your changes and reference the issue number.

---

## 💬 Questions & Feedback

If you have questions or need guidance on architecture, feel free to start a discussion or comment on an issue! We are excited to build CodeMonk together.
