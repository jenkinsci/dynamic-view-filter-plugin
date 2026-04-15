# Contributing to Dynamic View Filter Plugin

Thank you for your interest in contributing to the Dynamic View Filter plugin!

## Getting Started

1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone git@github.com:<your-username>/dynamic-view-filter-plugin.git
   cd dynamic-view-filter-plugin
   ```
3. Create a topic branch from `main`:
   ```bash
   git checkout -b feature/my-change
   ```

## Prerequisites

- **JDK 21** or newer
- **Maven 3.9+** (the repo includes a Maven wrapper in `.mvn/`)
- A working internet connection (dependencies are fetched from Maven Central and the Jenkins artifact repository)

## Building

```bash
mvn clean verify
```

This compiles the plugin, runs all tests, executes SpotBugs static analysis, and produces the HPI file at `target/dynamic-view-filter.hpi`.

To skip tests during development:

```bash
mvn clean package -DskipTests
```

## Running a Local Jenkins Instance

Start a local Jenkins with the plugin installed:

```bash
mvn hpi:run
```

Jenkins will be available at `http://localhost:8080/jenkins/`. Changes to Jelly files are hot-reloaded; Java changes require a restart.

## Running Tests

```bash
mvn test
```

The test suite uses the [Jenkins Test Harness](https://github.com/jenkinsci/jenkins-test-harness) which spins up a real Jenkins instance per test class. Tests cover:

- `DynamicBuildFilterColumnTest` — column wrapping, view resolution, run matching
- `DropdownFilterViewTest` — dropdown value extraction, job filtering, parameter-based run matching
- `InjectedTest` — automatic Jenkins extension point injection tests

## Code Style

This project uses [Spotless](https://github.com/diffplug/spotless) with the Jenkins community code style. Check formatting:

```bash
mvn spotless:check
```

Auto-fix formatting issues:

```bash
mvn spotless:apply
```

## Making Changes

1. Write your code changes.
2. Add or update tests to cover the change.
3. Run the full build to make sure everything passes:
   ```bash
   mvn clean verify
   ```
4. Commit with a clear, descriptive message:
   ```bash
   git commit -m "Add support for XYZ filtering"
   ```
5. Push to your fork and open a Pull Request against `main`.

## Pull Request Guidelines

- Keep PRs focused — one logical change per PR.
- Include tests for new functionality.
- Ensure `mvn clean verify` passes with zero warnings from SpotBugs.
- Update `README.md` or docs if the change affects user-facing behavior.
- Reference any related GitHub issues in the PR description.

## Reporting Issues

Open an issue on the [GitHub issue tracker](https://github.com/jenkinsci/dynamic-view-filter-plugin/issues) with:

- Jenkins version
- Plugin version
- Steps to reproduce
- Expected vs. actual behavior
- Relevant logs (from `Manage Jenkins > System Log`)

## Code of Conduct

This project follows the [Jenkins Code of Conduct](https://www.jenkins.io/project/conduct/).

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
