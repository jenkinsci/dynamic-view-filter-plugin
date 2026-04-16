# Usage Scenarios

This document walks through real-world scenarios where the Dynamic View Filter plugin adds value.
Each scenario describes the problem, the setup, and the expected outcome.

---

## Scenario 1: Filter a Multi-Environment Dashboard by Region

### Problem

You have a set of deployment jobs parameterized with a `region` parameter (`east`, `west`, `central`).
The default Jenkins list view shows the latest build regardless of region, so the "Last Success" column might show a `west` deployment even when you need to check the status of `east`.

### Setup

1. Create a new **List View** (or open an existing one).
2. Add a **Parameter Run Matcher Filter** to the Job Filters section:
   - Name Regex: `region`
   - Value Regex: `east`
   - Include/Exclude: `includeMatched`
   - Match All Builds: checked
3. Replace the default **Status** and **Last Success** columns with **Dynamic Build Filter Column** wrapping each:
   - Delegate: Status
   - Delegate: Last Success

### Expected Outcome

- The Status and Last Success columns now reflect only builds where `region=east`.
- If the last build was `region=west` but the previous build was `region=east`, the columns show data from that earlier east build.
- Jobs that have never been built with `region=east` show blank column cells.

---

## Scenario 2: Self-Contained Column Filtering Without View-Level Filters

### Problem

You want a single column to show the last successful build for `deploy_target=alpha`, but you don't want to add a view-level filter that affects all columns. Other columns should still show unfiltered data.

### Setup

1. Open the view configuration.
2. Add a **Parameter Build Filter Column**:
   - Parameter Name: `deploy_target`
   - Parameter Value Regex: `alpha`
   - Delegate: Last Success
3. Keep other columns (Status, Weather, etc.) as standard — no wrapper needed.

### Expected Outcome

- The Parameter Build Filter Column shows the last successful build where `deploy_target=alpha`.
- All other columns (Status, Weather) remain unfiltered and show the latest build data as usual.
- No view-level job filter is required.

---

## Scenario 3: Dropdown-Driven Project Dashboard

### Problem

You manage 50+ jobs organized in folders: `projects/frontend/build`, `projects/frontend/deploy`, `projects/backend/build`, `projects/backend/deploy`, etc. You want a single view where users can select a project from a dropdown and instantly see only that project's jobs.

### Setup

1. Create a new **Dropdown Filter View**.
2. Set **Include jobs by regex** to `projects/.*` and enable **Recurse in subfolders**.
3. Add a dropdown:
   - Label: `Project`
   - Source Type: **Job Name Regex**
   - Job Name Pattern: `projects/([^/]+)/.*`
4. Add columns as needed (Name, Status, Last Success, etc.).

### Expected Outcome

- The view renders with a "Project" dropdown at the top containing `frontend`, `backend`, and any other project names discovered from job paths.
- Selecting `frontend` filters the view to show only `projects/frontend/*` jobs.
- Selecting `(All)` (empty selection) shows all jobs again.

---

## Scenario 4: Combined Path + Parameter Dropdowns

### Problem

Jobs are organized by team (`teams/platform/...`, `teams/mobile/...`) and parameterized with an `env` parameter (`staging`, `production`). You want to filter by both team and environment simultaneously.

### Setup

1. Create a new **Dropdown Filter View**.
2. Set **Include jobs by regex** to `teams/.*` and enable **Recurse in subfolders**.
3. Add two dropdowns:
   - Dropdown 1:
     - Label: `Team`
     - Source Type: **Job Name Regex**
     - Job Name Pattern: `teams/([^/]+)/.*`
   - Dropdown 2:
     - Label: `Environment`
     - Source Type: **Build Parameter**
     - Parameter Name: `env`
4. Wrap the Status and Last Success columns with **Dynamic Build Filter Column**.

### Expected Outcome

- Two dropdowns appear: "Team" and "Environment".
- Selecting `platform` + `production` shows only platform team jobs, and the Status/Last Success columns show only builds where `env=production`.
- Dropdowns combine with AND logic — both conditions must be satisfied.
- The Environment dropdown scans actual build history to discover values (`staging`, `production`), so no manual configuration of options is needed.

---

## Scenario 5: Pipeline (Jenkinsfile) Jobs with Build Filtering

### Problem

The upstream `BuildFilterColumn` from View Job Filters breaks with Pipeline jobs due to reliance on XStream back-references and runtime class proxying.

### Setup

1. Create Pipeline jobs (Jenkinsfile-based) that accept parameters.
2. Create a list view with **Dynamic Build Filter Column** wrapping Status.
3. Add a **Parameter Run Matcher Filter** as a job filter.

### Expected Outcome

- Dynamic Build Filter Column resolves the parent view at render time from the Stapler request context instead of using stored XStream references.
- Pipeline jobs render correctly with filtered build data.
- FreeStyle, Matrix, and other job types continue to work as before.

---

## Scenario 6: Regex-Based Parameter Matching

### Problem

You have parameters with varying names across jobs (`deploy_region`, `target_region`, `aws_region`) and you want a single filter to match any of them when the value is `us-east-1`.

### Setup

1. Add a **Parameter Run Matcher Filter** to the view:
   - Name Regex: `.*region.*`
   - Value Regex: `us-east-1`
   - Include/Exclude: `includeMatched`
2. Wrap columns with **Dynamic Build Filter Column**.

### Expected Outcome

- The filter matches any parameter whose name contains "region" and whose value is `us-east-1`.
- All three naming variations (`deploy_region`, `target_region`, `aws_region`) are captured.
- Columns display only builds matching this criteria.

---

## Scenario 7: Exclude Specific Builds from View

### Problem

You want to see all builds *except* those targeting the `nightly` environment.

### Setup

1. Add a **Parameter Run Matcher Filter**:
   - Name Regex: `env`
   - Value Regex: `nightly`
   - Include/Exclude: `excludeMatched`
2. Wrap columns with **Dynamic Build Filter Column**.

### Expected Outcome

- Builds where `env=nightly` are excluded from the column data.
- All other builds (including those without an `env` parameter) are shown normally.

---

## Scenario 8: Default Value Matching (Unbuilt Jobs)

### Problem

Some jobs are defined with default parameter values but haven't been built yet. You want the view to include them based on their default parameter configuration.

### Setup

1. Add a **Parameter Run Matcher Filter**:
   - Name Regex: `env`
   - Value Regex: `production`
   - Use Default Value: **checked**
2. Configure the view normally.

### Expected Outcome

- Jobs with a `ChoiceParameterDefinition` or `StringParameterDefinition` named `env` with a default value of `production` are included, even if they have never been built.
- Jobs that have been built use the actual build parameter value instead.

---

## Quick Reference: Which Component to Use

| Goal | Component |
|---|---|
| Filter all columns in a view by build parameters | Parameter Run Matcher Filter + Dynamic Build Filter Column |
| Filter a single column by parameters (independent of other columns) | Parameter Build Filter Column |
| Add interactive dropdown filters to a view | Dropdown Filter View |
| Replace broken `BuildFilterColumn` for Pipeline jobs | Dynamic Build Filter Column |
| Filter by job folder path / naming convention | Dropdown Filter View with Job Name Regex source |
| Filter by actual build parameter values with auto-discovery | Dropdown Filter View with Build Parameter source |

---

## Scenario 9: Sidebar Filter Layout for Wide Tables

### Problem

You have many columns in the job table and placing the filter bar at the top pushes the table down too far, especially with many dropdown filters. You want the filters on the side so users can see both filters and the table at the same time.

### Setup

1. Open the **Dropdown Filter View** configuration.
2. Set **Filter Position** to **Sidebar**.
3. Save.

### Expected Outcome

- The filter bar appears as a vertical panel on the right side of the job table.
- Dropdowns are stacked vertically in the sidebar.
- The sidebar sticks to the viewport while scrolling (sticky positioning).
- Collapsing the sidebar reduces it to a compact icon strip, giving the table full width.

---

## Scenario 10: Multi-Level Folder Path Extraction

### Problem

Your jobs are organized in a deep folder hierarchy like `project/region/module/platform/component/action`. You want separate dropdowns for module, platform, component, and action — but each dropdown regex can only extract one capture group.

### Setup

1. Create a **Dropdown Filter View**.
2. Set **Include jobs by regex** to `project/region/.*/.*/.*/.*` and enable **Recurse in subfolders**.
3. Add four dropdowns, each extracting a different path segment into group 1:

| Label | Job Name Pattern |
|---|---|
| Module | `project/region/([^/]+)/.*` |
| Platform | `project/region/[^/]+/([^/]+)/.*` |
| Component | `project/region/[^/]+/[^/]+/([^/]+)/.*` |
| Action | `project/region/[^/]+/[^/]+/[^/]+/([^/]+)` |

Note: use `[^/]+` (non-capturing) for path segments you want to skip, and `([^/]+)` (capturing group 1) for the segment you want to extract.

### Expected Outcome

- Four dropdowns appear: Module, Platform, Component, Action.
- Each auto-populates with the distinct values from its respective path segment.
- Selecting `payments` + `linux` filters to only jobs for the payments module on Linux.
- All four dropdowns combine with AND logic.

---

## Scenario 11: Collapsible Filter Bar for Clean Dashboards

### Problem

You have a dashboard view on a wall-mounted monitor. Most of the time, users just want to see the job table without filter UI clutter. Occasionally they need to apply a filter.

### Setup

1. Configure the **Dropdown Filter View** with dropdowns (top or sidebar mode).
2. No special configuration needed — the collapse feature is built in.

### Expected Outcome

- The filter bar shows a **Filters** toggle button (options icon) and a **Reset** button.
- Clicking the toggle collapses the filter bar: in top mode the dropdowns hide and only icons remain; in sidebar mode the sidebar shrinks to a compact icon strip.
- The collapse state persists across page loads via `localStorage`.
- Clicking the toggle again expands the filters back.
