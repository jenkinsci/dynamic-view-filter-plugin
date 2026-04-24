# Screenshots

## Dropdown Filter View — Top Mode

Filter bar at the top with horizontally arranged dropdowns, Reset button, and Filters toggle.

![Top Mode — Expanded](images/dropdown-filter-view-top-expanded.png)

With a filter applied (Project = `plugins`), only matching jobs are shown:

![Top Mode — Filtered](images/dropdown-filter-view-top-filtered.png)

Collapsed state, only the toggle icon remains:

![Top Mode — Collapsed](images/dropdown-filter-view-top-collapsed.png)

## Dropdown Filter View — Sidebar Mode

Filter bar as a sidebar on the right with vertically stacked dropdowns.

![Sidebar Mode — Expanded](images/dropdown-filter-view-sidebar-expanded.png)

With a filter applied (Project = `jenkins`), only matching jobs are shown:

![Sidebar Mode — Filtered](images/dropdown-filter-view-sidebar-filtered.png)

Collapsed state, sidebar collapses to a compact icon strip:

![Sidebar Mode — Collapsed](images/dropdown-filter-view-sidebar-collapsed.png)

## Configuration

### Dropdown Filters

Add dropdown definitions with Job Name Regex or Build Parameter source types.

![Dropdown Filters Config](images/dropdown-view-config-dropdown-filters.png)

### Filter Position

Choose where the filter bar appears: top or sidebar.

![Filter Position Config](images/dropdown-view-config-filter-position.png)

### Job Name Regex Source

Extract dropdown values from job folder paths using a regex capture group.

![Job Name Regex Filter](images/dropdown-view-job-name-regex-filter.png)

### Build Parameter Source

Populate dropdown values from actual build parameter values.

![Build Parameter Filter](images/dropdown-view-build-parameter-filter.png)

### Columns

Configure Dynamic Build Filter Column and Parameter Build Filter Column.

![Columns Config](images/dropdown-view-config-columns.png)

### Job Filters

Add Parameter Run Matcher Filter to the Job Filters section.

![Job Filters Config](images/dropdown-view-config-job-filters.png)

## Column and Filter Types

### Dynamic Build Filter Column

Wraps a standard column (Status, Weather, etc.) and filters build data through the view's job filters.

![Dynamic Build Filter Column](images/dynamic-build-filter-column.png)

### Parameter Build Filter Column

Self-contained column that filters builds by parameter name and value regex, with delegate column selection.

![Parameter Build Filter Column](images/parameter-build-filter-column.png)

### Parameter Run Matcher Filter

View-level job filter for matching builds by parameter name, value, and description regex.

![Parameter Run Matcher Filter](images/parameter-run-matcher-filter.png)
