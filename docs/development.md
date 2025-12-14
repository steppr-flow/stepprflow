# Development Guide

This guide covers setting up a local development environment for Steppr Flow.

## Prerequisites

- **Java 21** (Temurin/OpenJDK recommended)
- **Maven 3.9+**
- **Node.js 20+** (for UI development)
- **Docker** (for integration tests)
- **Git**

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/steppr-flow/steppr-flow.git
cd steppr-flow
```

### 2. Build all modules

```bash
mvn clean install
```

### 3. Run tests

```bash
mvn test
```

## Project Structure

```
steppr-flow/
├── .github/                 # GitHub Actions workflows
│   ├── workflows/
│   │   ├── ci.yml          # CI pipeline
│   │   └── release.yml     # Release automation
│   └── dependabot.yml      # Dependency updates
├── config/
│   └── checkstyle/         # Checkstyle configuration
├── docs/                   # Documentation
├── steppr-flow-core/       # Core module
├── steppr-flow-spring-*/   # Spring integrations
├── steppr-flow-ui/         # Vue.js frontend
└── pom.xml                 # Parent POM
```

## Building Individual Modules

Build a specific module:
```bash
mvn -pl steppr-flow-core clean install
```

Build with dependencies:
```bash
mvn -pl steppr-flow-spring-kafka -am clean install
```

## Running the Sample Applications

### Kafka Sample

1. Start infrastructure:
```bash
cd steppr-flow-kafka-sample
docker-compose up -d
```

2. Run the application:
```bash
mvn spring-boot:run
```

3. Test the API:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": "PROD-001", "quantity": 2}'
```

### RabbitMQ Sample

1. Start infrastructure:
```bash
cd steppr-flow-rabbitmq-sample
docker-compose up -d
```

2. Run the application:
```bash
mvn spring-boot:run
```

## Running the Dashboard

### Option 1: Standalone Dashboard

```bash
cd steppr-flow-dashboard
docker-compose up -d  # Start Kafka & MongoDB
mvn spring-boot:run
```

Access at: http://localhost:8080

### Option 2: With Docker

```bash
docker-compose up -d
```

## Frontend Development

### Setup

```bash
cd steppr-flow-ui
npm install
```

### Development server

```bash
npm run dev
```

Access at: http://localhost:5173

### Build for production

```bash
npm run build
```

### Run tests

```bash
npm run test
npm run test:e2e  # Cypress E2E tests
```

## Code Quality

### Checkstyle

Run checkstyle validation:
```bash
mvn checkstyle:check
```

### SpotBugs

Run static analysis:
```bash
mvn spotbugs:check
```

### All quality checks

```bash
mvn verify -DskipTests
```

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

Integration tests require Docker:
```bash
mvn verify
```

### Coverage Report

```bash
mvn jacoco:report
```

Reports are generated in `target/site/jacoco/index.html`.

### Load Tests

```bash
cd steppr-flow-load-tests
mvn gatling:test
```

## IDE Setup

### IntelliJ IDEA

1. Import as Maven project
2. Enable annotation processing (for Lombok)
3. Install plugins:
   - Lombok
   - CheckStyle-IDEA
   - Spring Boot

### VS Code

Recommended extensions:
- Extension Pack for Java
- Spring Boot Extension Pack
- Vue - Official
- Tailwind CSS IntelliSense

## Common Tasks

### Adding a new module

1. Create module directory
2. Add `pom.xml` with parent reference:
```xml
<parent>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>steppr-flow-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

3. Add module to parent `pom.xml`:
```xml
<modules>
    ...
    <module>new-module</module>
</modules>
```

### Updating dependencies

Update a single dependency:
```bash
mvn versions:use-latest-versions -Dincludes=groupId:artifactId
```

Update all dependencies:
```bash
mvn versions:use-latest-versions
```

Check for updates:
```bash
mvn versions:display-dependency-updates
```

### Creating a release

Releases are automated via GitHub Actions:

1. Go to **Actions** > **Release**
2. Click **Run workflow**
3. Select version type (patch/minor/major)
4. Optionally mark as prerelease

Or manually:
```bash
mvn versions:set -DnewVersion=1.0.0
mvn clean deploy
git tag v1.0.0
git push --tags
```

## Troubleshooting

### Maven build fails

```bash
mvn clean install -U  # Force update snapshots
```

### Tests fail with Docker

Ensure Docker is running:
```bash
docker info
```

### Checkstyle errors

Run with details:
```bash
mvn checkstyle:check -Dcheckstyle.consoleOutput=true
```

### Port conflicts

Default ports:
- Dashboard: 8080
- MongoDB: 27017
- Kafka: 9092
- RabbitMQ: 5672, 15672

## Contributing

1. Create a feature branch from `develop`
2. Make your changes
3. Run all checks: `mvn verify`
4. Create a pull request

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed guidelines.
