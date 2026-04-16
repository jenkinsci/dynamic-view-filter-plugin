# Build Process

This document describes how to build, test, and package the Dynamic View Filter plugin.

## Project Structure

```
dynamic-view-filter-plugin/
├── pom.xml                          # Maven project descriptor
├── Jenkinsfile                      # CI pipeline definition
├── LICENSE
├── README.md
├── CONTRIBUTING.md
├── src/
│   ├── main/
│   │   ├── java/io/jenkins/plugins/dynamic_view_filter/
│   │   │   ├── DynamicBuildFilterColumn.java    # View-aware build filter column
│   │   │   ├── ParameterBuildFilterColumn.java  # Self-contained parameter filter column
│   │   │   ├── ParameterRunMatcherFilter.java   # View-level RunMatcher job filter
│   │   │   ├── DropdownFilterView.java          # ListView with dropdown filters
│   │   │   ├── DropdownDefinition.java          # Single dropdown configuration
│   │   │   ├── FilteredJob.java                 # Shared Job wrapper with predicate-based filtering
│   │   │   └── ParameterUtils.java              # Shared parameter extraction utilities
│   │   └── resources/
│   │       ├── index.jelly                      # Plugin description for Jenkins UI
│   │       └── io/jenkins/plugins/dynamic_view_filter/
│   │           ├── DynamicBuildFilterColumn/
│   │           │   ├── config.jelly             # Column configuration UI
│   │           │   ├── column.jelly             # Column rendering
│   │           │   ├── columnHeader.jelly        # Column header rendering
│   │           │   └── index.jelly              # Extension point description
│   │           ├── ParameterBuildFilterColumn/
│   │           │   ├── config.jelly
│   │           │   ├── column.jelly
│   │           │   ├── columnHeader.jelly
│   │           │   └── index.jelly              # Extension point description
│   │           ├── ParameterRunMatcherFilter/
│   │           │   ├── config.jelly
│   │           │   └── index.jelly              # Extension point description
│   │           ├── DropdownFilterView/
│   │           │   ├── main.jelly               # View rendering with dropdowns
│   │           │   ├── view.js                  # CSP-compliant external JS for filter bar
│   │           │   ├── configure-entries.jelly   # View configuration form
│   │           │   └── newViewDetail.jelly       # Create View dialog description
│   │           ├── DropdownDefinition/
│   │           │   ├── config.jelly             # Dropdown source type config
│   │           │   └── config.js                # CSP-compliant external JS for field toggling
│   │           └── Messages.properties          # Display name strings
│   └── test/java/io/jenkins/plugins/dynamic_view_filter/
│       ├── DynamicBuildFilterColumnTest.java     # 10 tests
│       └── DropdownFilterViewTest.java           # 8 tests
├── .github/
│   ├── dependabot.yml                           # Automated dependency updates
│   ├── CODEOWNERS
│   └── workflows/
│       ├── cd.yaml                              # Continuous delivery
│       └── jenkins-security-scan.yml            # Security scanning
├── .mvn/
│   ├── extensions.xml                           # git-changelist-maven-extension
│   └── maven.config                             # Default Maven flags
└── docs/
    ├── BUILD.md                                 # This file
    ├── USAGE_SCENARIOS.md                       # Detailed usage scenarios
    └── images/                                  # Screenshots for README
        ├── dropdown-filter-view.png
        ├── dropdown-filter-view-filtered-01.png
        ├── dropdown-view-build-parameter-filter.png
        ├── dropdown-view-job-name-regex-filter.png
        ├── dynamic-build-filter-column.png
        ├── parameter-build-filter-column.png
        └── parameter-run-matcher-filter.png
```

## Requirements

| Tool | Minimum Version |
|------|----------------|
| JDK  | 21             |
| Maven | 3.9+          |
| Jenkins (runtime) | 2.528.3 |

The Maven wrapper in `.mvn/` ensures a compatible Maven version is used in CI.

## Build Commands

### Full Build (compile + test + static analysis + package)

```bash
mvn clean verify
```

This runs:
1. **compile** — Compiles Java sources
2. **test** — Runs 36 unit/integration tests via Jenkins Test Harness
3. **spotbugs:check** — Static analysis for common bug patterns
4. **spotless:check** — Code formatting validation
5. **package** — Produces `target/dynamic-view-filter.hpi`

### Quick Build (skip tests)

```bash
mvn clean package -DskipTests
```

### Run Tests Only

```bash
mvn test
```

### Run a Single Test Class

```bash
mvn test -Dtest=DynamicBuildFilterColumnTest
```

### Run a Single Test Method

```bash
mvn test -Dtest=DynamicBuildFilterColumnTest#pipelineJobBuildStatusFiltered
```

### Fix Code Formatting

```bash
mvn spotless:apply
```

### Check Code Formatting

```bash
mvn spotless:check
```

## Local Development

### Start Jenkins with the Plugin

```bash
mvn hpi:run
```

Opens Jenkins at `http://localhost:8080/jenkins/` with the plugin pre-installed. Jelly/resource changes are hot-reloaded; Java changes require a restart (`Ctrl+C`, then re-run).

### Debug Mode

```bash
mvnDebug hpi:run
```

Starts Jenkins in debug mode, listening on port 8000 for a remote debugger.

## Packaging

### Build the HPI File

```bash
mvn clean package
```

The installable plugin file is produced at:

```
target/dynamic-view-filter.hpi
```

Upload this file via **Manage Jenkins → Plugins → Advanced → Deploy Plugin** to install manually.

## Dependency Management

Dependencies are managed via the Jenkins BOM (Bill of Materials):

- **Parent POM:** `org.jenkins-ci.plugins:plugin:6.2152.ve00a_731c3ce9`
- **BOM:** `io.jenkins.tools.bom:bom-2.528.x:6269.v7a_159d68a_366`

The BOM controls versions of all Jenkins core and plugin dependencies. The only explicit dependency is `view-job-filters` (version managed by BOM). Test-scoped dependencies include `workflow-job`, `workflow-cps`, `workflow-basic-steps`, `workflow-durable-task-step`, and `jenkins-test-harness`.

### Update Dependencies

To check for available updates:

```bash
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

## Continuous Integration

### Jenkinsfile

The CI pipeline runs on the Jenkins infrastructure:

```groovy
buildPlugin(useContainerAgent: true, configurations: [
  [platform: 'linux', jdk: 21],
  [platform: 'windows', jdk: 21],
])
```

### GitHub Actions

- **cd.yaml** — Continuous delivery, triggered on check_run completion
- **jenkins-security-scan.yml** — Automated security scanning on push and PR

## Versioning

This plugin uses [Jenkins Incrementals](https://github.com/jenkinsci/incrementals-tools):

- Development builds use `999999-SNAPSHOT` as the version.
- Release versions are computed automatically from Git tags via `git-changelist-maven-extension`.
- The CD workflow handles automated releases to the Jenkins Update Center.

## Troubleshooting

### Tests fail with `ClassNotFoundException`

Run `mvn clean` to clear stale compiled classes, then rebuild.

### SpotBugs reports false positives

Add a `@SuppressFBWarnings` annotation with a justification:

```java
@SuppressFBWarnings(value = "RULE_ID", justification = "Reason for suppression")
```

### SSL errors downloading from repo.jenkins-ci.org

Import the Jenkins repo certificate into your JDK truststore:

```bash
openssl s_client -connect repo.jenkins-ci.org:443 -showcerts </dev/null 2>/dev/null \
  | openssl x509 -outform PEM > /tmp/jenkins-repo.pem
sudo keytool -import -trustcacerts \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit -noprompt \
  -alias jenkins-repo -file /tmp/jenkins-repo.pem
```
