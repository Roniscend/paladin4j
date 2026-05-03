# Contributing to Paladin Java SDK

Thank you for your interest in contributing to the Paladin Java SDK! We welcome pull requests, bug reports, and feature requests.

## Development Setup

1. **Prerequisites**
   - Java 17 or later
   - Docker (for running integration tests)

2. **Clone the Repository**
   ```bash
   git clone https://github.com/LFDT-Paladin/paladin-java-sdk.git
   cd paladin-java-sdk
   ```

3. **Build the Project**
   ```bash
   ./gradlew build
   ```

## Testing Guidelines

We enforce a strict testing policy to ensure SDK reliability in enterprise environments.

- **Unit Tests:** Must be written for all new features using JUnit 5, Mockito, and AssertJ. Run them with `./gradlew test`.
- **Integration Tests:** Any feature interacting with a Paladin node must include an integration test using Testcontainers. Run them with `./gradlew integrationTest`.
- **Coverage:** We use JaCoCo for code coverage. Run `./gradlew test jacocoTestReport` and ensure your changes are adequately covered.

## Pull Request Process

1. Fork the repository and create your feature branch: `git checkout -b feature/my-new-feature`
2. Write tests for your changes.
3. Ensure the build passes locally: `./gradlew clean build integrationTest`
4. Commit your changes. Please use clear, descriptive commit messages.
5. Push to your branch and submit a Pull Request.

## Code Style

- We follow standard Java conventions.
- Use Java 17 `record` types for immutable data models.
- All public API methods and classes must have JavaDoc.
- Do not add new external dependencies unless strictly necessary (e.g., prefer JDK 17 `java.net.http.HttpClient` over OkHttp).

## Reporting Issues

If you find a bug, please open an issue providing:
- SDK version
- Java version
- Paladin node version
- Steps to reproduce the issue
- Expected vs actual behavior
