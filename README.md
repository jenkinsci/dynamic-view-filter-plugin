# Dynamic View Filter Plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fdynamic-view-filter-plugin%2Fmain)](https://ci.jenkins.io/job/Plugins/job/dynamic-view-filter-plugin/job/main/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/dynamic-view-filter.svg)](https://plugins.jenkins.io/dynamic-view-filter)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/dynamic-view-filter.svg?color=blue)](https://plugins.jenkins.io/dynamic-view-filter)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/dynamic-view-filter-plugin.svg)](https://github.com/jenkinsci/dynamic-view-filter-plugin/graphs/contributors)

Dynamic view filters with auto-populated dropdown menus, build filter columns, and parameter-based run matching for Jenkins.

## What does this plugin do?

Imagine you have hundreds of Jenkins jobs organized in folders, different projects, platforms, and environments. Normally you'd scroll through all of them or create dozens of separate views manually.

This plugin lets you:

- **Add dropdown menus at the top of any view** that automatically populate with values extracted from your job names or build parameters. Select "production" from an environment dropdown and instantly see only production jobs.
- **Show only relevant build results in columns.** If you filter by a parameter (e.g., `region=east`), the Status, Last Success, and Last Failure columns update to reflect only matching builds — not just the latest build regardless.
- **Works with all job types.** The upstream `BuildFilterColumn` from View Job Filters can break with Pipeline (Jenkinsfile) jobs. This plugin handles Pipeline, FreeStyle, Matrix, and any other job type reliably.

![Dropdown Filter View — Top Mode](docs/images/dropdown-filter-view-top-filtered.png)

![Dropdown Filter View — Sidebar Mode](docs/images/dropdown-filter-view-sidebar-filtered.png)

## Features

### Dropdown Filter View

A custom view type extending ListView with configurable dropdown filters. Dropdowns auto-populate from job names (via regex capture groups) or build parameter values. Multiple dropdowns combine with AND logic. The filter bar can be positioned at the **top** or as a **sidebar**, and is collapsible.

> **Important:** You must configure **Include jobs by regex** (e.g., `.*`) and enable **Recurse in subfolders** for the dropdowns to discover jobs.

### Dynamic Build Filter Column

A drop-in replacement for `BuildFilterColumn`. Wraps any standard column (Status, Weather, Last Success, etc.) and filters build data through the view's `RunMatcher` job filters before the delegate renders.

### Parameter Build Filter Column

A self-contained column that filters builds by parameter name and value regex. No separate view-level filter required — configure the parameter matching directly on the column.

### Parameter Run Matcher Filter

A view-level job filter implementing `RunMatcher`. Add it once to a view's Job Filters section and every `DynamicBuildFilterColumn` in that view will filter builds through it. Supports name/value/description regex, default vs. build value matching, multi-build scanning, and include/exclude modes.

## Example Regex Patterns

| Use Case | Include Jobs Regex | Dropdown Job Name Pattern |
|---|---|---|
| Jobs in `projects/` folders | `projects/.*` | `projects/([^/]+)/.*` → extracts project name |
| Two-level: team + service | `teams/.*` | `teams/([^/]+)/.*` → extracts team name |
| Skip a path segment | `org/region/.*` | `org/region/[^/]+/([^/]+)/.*` → extracts 2nd segment |
| Match all jobs | `.*` | _(use Build Parameter source instead)_ |

**Parameter Run Matcher examples:**

| Name Regex | Value Regex | Effect |
|---|---|---|
| `region` | `east` | Exact match on `region=east` |
| `.*region.*` | `us-east-1` | Any param containing "region" with value `us-east-1` |
| `env` | `prod\|staging` | Match `env=prod` or `env=staging` |
| `deploy_target` | `(?!nightly).*` | Exclude `nightly` via negative lookahead |

## Getting Started

1. Create a new view and select **Dropdown Filter View** (or use a standard **List View**)
2. Add **Dynamic Build Filter Column** wrapping your desired columns (Status, Last Success, etc.)
3. Optionally add **Parameter Run Matcher Filter** to the Job Filters section
4. For Dropdown Filter View, add dropdowns with regex patterns or parameter names

## Requirements

- Jenkins 2.528.3 or newer
- [View Job Filters](https://plugins.jenkins.io/view-job-filters/) plugin

## Documentation

- [Usage Scenarios](docs/USAGE_SCENARIOS.md) — walkthroughs for multi-environment dashboards, dropdown-driven views, Pipeline filtering, and more
- [Screenshots](docs/SCREENSHOTS.md) — all screenshots including configuration UI
- [Troubleshooting](docs/TROUBLESHOOTING.md) — common issues and regex tips
- [Build Guide](docs/BUILD.md) — development setup and build commands

## Changelog

See [GitHub Releases](https://github.com/jenkinsci/dynamic-view-filter-plugin/releases) for the changelog.

## Contributing

Refer to [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, build commands, and pull request guidelines.

## License

Licensed under the [MIT License](LICENSE).
