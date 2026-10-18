# Upgrade Plan: metamorfosis (20261008164202)

- **Generated**: 2026-10-08 16:42:02 UTC
- **HEAD Branch**: Reegistro-Usuario
- **HEAD Commit ID**: N/A

## Available Tools

**JDKs**
- JDK 21.0.2: C:\Users\santi\.jdks\graalvm-ce-21.0.2\bin (current project JDK, used by baseline step)
- JDK 25.0.2: C:\Users\santi\.jdks\openjdk-25.0.2\bin (target runtime, used by upgrade and final validation)

**Build Tools**
- Maven Wrapper: 3.9.16 (compatible with Java 25; no upgrade required)
- Maven Wrapper executable: backend/mvnw.cmd

## Guidelines

- Upgrade only the Java runtime target from 21 to the latest LTS, Java 25.
- Preserve existing authentication changes and all application behavior.
- Use the Maven Wrapper and Java 25 for final validation.
- Run tests before and after the upgrade.

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: appmod/java-upgrade-20261008164202
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java 25 LTS runtime and compilation target.

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| --- | --- | --- | --- |
| Java | 21 | 25 | User requested latest LTS target |
| Spring Boot | 4.1.1 | 4.1.1 | Already compatible with Java 25; no framework upgrade required |
| Maven Wrapper | 3.9.16 | 3.9.16 | Supports the target JDK; no upgrade required |
| maven-compiler-plugin | Managed by Spring Boot 4.1.1 | Managed version | Existing plugin configuration is compatible with Java 25 |
| Lombok | Managed by Spring Boot 4.1.1 | Managed version | Existing annotation processor configuration remains compatible |
| Spring Security | Managed by Spring Boot 4.1.1 | Managed version | Existing Java 21 configuration does not require a major migration |

## Derived Upgrades

- Java 21 is upgraded to Java 25 because Java 25 is the latest LTS.
- The Maven compiler release target is updated through the existing `java.version` property; no source-level migration is required.
- No Spring Boot, Spring Framework, Jakarta EE, or test-framework upgrade is required.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
| --- | --- | --- | --- | --- | --- |
| backend/pom.xml | java.version | 21 | upgrade | 25 | User requested latest LTS runtime |
| backend/pom.xml | Spring Boot parent | 4.1.1 | keep | 4.1.1 | Already compatible with Java 25; upgrading would be unnecessary |
| backend/.mvn/wrapper/maven-wrapper.properties | Maven Wrapper | 3.9.16 | keep | 3.9.16 | Compatible with Java 25; no wrapper upgrade required |

### Source Code Changes

| File | Location | Current | Required Change | Reason |
| --- | --- | --- | --- | --- |
| backend/pom.xml | `<java.version>` | 21 | Change to 25 | Sets the compiler and runtime release target to Java 25 |

### Configuration Changes

| File | Property/Setting | Current | Required Change | Reason |
| --- | --- | --- | --- | --- |
| backend/pom.xml | `java.version` | 21 | Change to 25 | Java 25 is the requested LTS target |

### CI/CD Changes

| File | Location | Current | Required Change |
| --- | --- | --- | --- |
| None found | N/A | N/A | No CI/CD Java version references were found |

### Risks & Warnings

- **Java 25 compiler/runtime behavior**: Java 25 may expose stricter reflection or module-access checks in existing code. The scan found no `jdk.internal`, `sun.*`, or `setAccessible` usage in application sources. A full compile and test run will verify this risk.
- **Existing uncommitted changes**: Authentication files already contain local modifications. Branch preparation will preserve them and the upgrade will avoid unrelated edits.

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Ensure the target JDK and Maven Wrapper are available before changing the project.
  - **Changes to Make**: No project files are changed. Confirm Java 25 and the Maven Wrapper are usable.
  - **Verification**: `C:\Users\santi\.jdks\openjdk-25.0.2\bin\java.exe -version` and `backend\mvnw.cmd -version`.

- Step 2: Setup Baseline
  - **Rationale**: Record the pre-upgrade compilation and test status with Java 21.
  - **Changes to Make**: No project files are changed.
  - **Verification**: `backend\mvnw.cmd clean compile test-compile -q && backend\mvnw.cmd clean test -q` using Java 21.

- Step 3: Upgrade Java Target
  - **Rationale**: Move the project compiler and runtime release target from Java 21 to Java 25.
  - **Changes to Make**: Update `backend/pom.xml` `<java.version>` from 21 to 25.
  - **Verification**: `backend\mvnw.cmd clean test-compile -q` using Java 25.

- Step 4: Final Validation
  - **Rationale**: Verify the complete Java 25 build and all tests after the target change.
  - **Changes to Make**: Resolve any compile or test failures caused by the Java 25 target.
  - **Verification**: `backend\mvnw.cmd clean test -q`, followed by `backend\mvnw.cmd clean verify -Djacoco.skip=false` when supported by the project.
