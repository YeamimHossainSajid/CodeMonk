#!/usr/bin/env python3
import json
import re
import subprocess
import sys
import os

def fetch_issues():
    print("Fetching open issues from GitHub...")
    cmd = ["gh", "issue", "list", "--state", "open", "--limit", "500", "--json", "number,title,labels,body"]
    res = subprocess.check_output(cmd)
    return json.loads(res)

def determine_issue_details(issue):
    title = issue['title'].strip()
    num = issue['number']
    
    module = "libs/common-core"
    package = "com.codemonk.common"
    file_path = ""
    class_name = ""
    instructions = ""
    
    # 1. ArchitectureQualityTest_X
    m = re.match(r"Add (ArchitectureQualityTest_\d+) test class", title)
    if m:
        class_name = m.group(1)
        module = "libs/common-core"
        package = "com.codemonk.common.arch"
        file_path = f"libs/common-core/src/test/java/com/codemonk/common/arch/{class_name}.java"
        instructions = (
            f"1. Create the ArchUnit test class `{class_name}` extending or using ArchUnit rules.\n"
            f"2. Add test assertions enforcing package encapsulation, layered architecture boundaries, or naming conventions.\n"
            f"3. Run unit test via `./mvnw test -pl libs/common-core -Dtest={class_name}`."
        )
        return module, package, file_path, class_name, instructions

    # 2. Mock clients
    m = re.match(r"Create (Mock\w+) for test environments", title)
    if m:
        class_name = m.group(1)
        if "Ai" in class_name:
            module = "services/ai-service"
            package = "com.codemonk.ai.client.mock"
            file_path = f"services/ai-service/src/test/java/com/codemonk/ai/client/mock/{class_name}.java"
        elif "Knowledge" in class_name:
            module = "services/knowledge-service"
            package = "com.codemonk.knowledge.client.mock"
            file_path = f"services/knowledge-service/src/test/java/com/codemonk/knowledge/client/mock/{class_name}.java"
        elif "Search" in class_name:
            module = "services/search-service"
            package = "com.codemonk.search.client.mock"
            file_path = f"services/search-service/src/test/java/com/codemonk/search/client/mock/{class_name}.java"
        else:
            module = "libs/common-core"
            package = "com.codemonk.common.test"
            file_path = f"libs/common-core/src/test/java/com/codemonk/common/test/{class_name}.java"
            
        instructions = (
            f"1. Create mock implementation `{class_name}` providing deterministic test responses.\n"
            f"2. Implement target client interface or extend base client.\n"
            f"3. Verify in test suite for the `{module}` module."
        )
        return module, package, file_path, class_name, instructions

    # 3. RedisCacheTest_X
    m = re.match(r"Add (RedisCacheTest_\d+) unit test", title)
    if m:
        class_name = m.group(1)
        module = "libs/common-core"
        package = "com.codemonk.common.cache"
        file_path = f"libs/common-core/src/test/java/com/codemonk/common/cache/{class_name}.java"
        instructions = (
            f"1. Create unit test `{class_name}` testing Redis cache key generation, TTL expiration, or cache eviction rules.\n"
            f"2. Mock `RedisTemplate` or use embedded test Redis.\n"
            f"3. Run test via `./mvnw test -pl libs/common-core -Dtest={class_name}`."
        )
        return module, package, file_path, class_name, instructions

    # 4. KafkaEventTest_X
    m = re.match(r"Add (KafkaEventTest_\d+) unit test", title)
    if m:
        class_name = m.group(1)
        module = "libs/common-core"
        package = "com.codemonk.common.event"
        file_path = f"libs/common-core/src/test/java/com/codemonk/common/event/{class_name}.java"
        instructions = (
            f"1. Create unit test `{class_name}` verifying Kafka event serialization, topic metadata, or event payload validation.\n"
            f"2. Use `KafkaTestUtils` or mock `KafkaTemplate`.\n"
            f"3. Run test via `./mvnw test -pl libs/common-core -Dtest={class_name}`."
        )
        return module, package, file_path, class_name, instructions

    # 5. Create class (generic Create <ClassName>...)
    m = re.match(r"Create (\w+)(?: in (\S+))?", title)
    if m:
        class_name = m.group(1)
        target_hint = m.group(2) if m.group(2) else ""
        
        if "common-core" in target_hint or "Cache" in class_name or "Redis" in class_name or "Dlq" in class_name or "Arch" in class_name or "WireMock" in class_name or "Integration" in class_name or "Testcontainers" in class_name:
            module = "libs/common-core"
            if "Cache" in class_name or "Redis" in class_name:
                package = "com.codemonk.common.cache"
                folder = "src/main/java" if "Test" not in class_name else "src/test/java"
            elif "Dlq" in class_name or "Event" in class_name or "Kafka" in class_name:
                package = "com.codemonk.common.event"
                folder = "src/main/java" if "Test" not in class_name else "src/test/java"
            elif "Arch" in class_name:
                package = "com.codemonk.common.arch"
                folder = "src/test/java"
            elif "Testcontainers" in class_name or "Config" in class_name:
                package = "com.codemonk.common.config"
                folder = "src/test/java" if "Test" in class_name or "Config" in class_name else "src/main/java"
            else:
                package = "com.codemonk.common.test"
                folder = "src/test/java"
            file_path = f"{module}/{folder}/{package.replace('.', '/')}/{class_name}.java"
        elif "Repository" in class_name:
            module = "services/repository-service"
            package = "com.codemonk.repository.service"
            file_path = f"{module}/src/main/java/com/codemonk/repository/service/{class_name}.java"
        else:
            module = "libs/common-core"
            package = "com.codemonk.common.service"
            file_path = f"{module}/src/main/java/com/codemonk/common/service/{class_name}.java"

        instructions = (
            f"1. Create Java component/class `{class_name}` in package `{package}`.\n"
            f"2. Annotate with appropriate Spring stereotype (`@Service`, `@Component`, `@Configuration`, or test annotations).\n"
            f"3. Add unit tests and verify module compilation using `./mvnw test -pl {module}`."
        )
        return module, package, file_path, class_name, instructions

    # 6. Gateway / Eureka / Config title formats
    if "Gateway" in title:
        module = "services/api-gateway"
        package = "com.codemonk.gateway.config"
        class_name = title.replace(" ", "")
        file_path = f"services/api-gateway/src/main/java/com/codemonk/gateway/config/{class_name}.java"
        instructions = (
            f"1. Implement Spring Cloud Gateway configuration in package `{package}`.\n"
            f"2. Configure routes, predicates, or filter beans in `services/api-gateway`.\n"
            f"3. Verify gateway startup and routes using `./mvnw test -pl services/api-gateway`."
        )
        return module, package, file_path, class_name, instructions

    if "Eureka" in title:
        module = "services/discovery-server"
        package = "com.codemonk.discovery.config"
        class_name = title.replace(" ", "")
        file_path = f"services/discovery-server/src/main/java/com/codemonk/discovery/config/{class_name}.java"
        instructions = (
            f"1. Configure Eureka server discovery properties in package `{package}`.\n"
            f"2. Update `application.yml` or java config beans in `services/discovery-server`.\n"
            f"3. Verify eureka server compilation using `./mvnw test -pl services/discovery-server`."
        )
        return module, package, file_path, class_name, instructions

    # Generic Fallback
    words = [w for w in re.findall(r'[A-Za-z0-9]+', title) if w not in ['Add', 'Create', 'Configure', 'unit', 'test', 'class', 'for']]
    class_name = "".join(w.capitalize() for w in words) if words else f"Issue{num}Task"
    module = "libs/common-core"
    package = "com.codemonk.common"
    file_path = f"libs/common-core/src/main/java/com/codemonk/common/{class_name}.java"
    instructions = (
        f"1. Create `{class_name}` in module `{module}` under package `{package}`.\n"
        f"2. Implement required business logic and add test coverage.\n"
        f"3. Verify build with `./mvnw test -pl {module}`."
    )
    return module, package, file_path, class_name, instructions

def format_issue_body(issue, module, package, file_path, class_name, instructions):
    orig_body = issue.get('body', '') or ""
    # Strip existing metadata if present
    if "### 🎯 Implementation Details" in orig_body:
        orig_body = orig_body.split("### 🎯 Implementation Details")[0].strip()

    body = f"""{orig_body}

### 🎯 Implementation Details & Location Guide

- **Target Microservice / Module**: `{module}`
- **Package Name**: `{package}`
- **Target File Path**: [`{file_path}`](file:///{file_path})
- **Class / Component Name**: `{class_name}`

---

### 🛠️ Step-by-Step Implementation Guide

{instructions}

---

### 🧪 How to Verify & Test
```bash
# Run unit tests for the target module
./mvnw test -pl {module}
```
"""
    return body

def main():
    dry_run = "--dry-run" in sys.argv
    issues = fetch_issues()
    print(f"Total open issues to process: {len(issues)}")
    
    updated_count = 0
    for idx, issue in enumerate(issues):
        num = issue['number']
        module, package, file_path, class_name, instructions = determine_issue_details(issue)
        new_body = format_issue_body(issue, module, package, file_path, class_name, instructions)
        
        if dry_run:
            if idx < 5:
                print(f"\n--- [DRY-RUN] Issue #{num}: {issue['title']} ---")
                print(f"Module: {module}")
                print(f"Package: {package}")
                print(f"File Path: {file_path}")
        else:
            print(f"[{idx+1}/{len(issues)}] Updating Issue #{num}: {issue['title']}...")
            try:
                subprocess.check_call(["gh", "issue", "edit", str(num), "--body", new_body])
                updated_count += 1
            except Exception as e:
                print(f"Failed to update issue #{num}: {e}")
                
    print(f"\nDone! Processed {len(issues)} issues. Updated: {updated_count if not dry_run else 0} (Dry Run: {dry_run})")

if __name__ == "__main__":
    main()
