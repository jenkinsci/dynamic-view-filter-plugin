# Troubleshooting

## Columns show N/A even though jobs have been built

`DynamicBuildFilterColumn` filters build data through **all** `RunMatcher` filters configured on the view — not just dropdown selections. If you have a `Parameter Run Matcher Filter` in the view's **Filters** section (e.g., `LAB=remote`), every build that doesn't match that filter will be excluded, and the columns will show N/A.

**To diagnose:**
1. Go to **Edit View** → **Job Filters** section
2. Check if any `Parameter Run Matcher Filter` is configured with restrictive criteria
3. If the jobs don't have matching parameter values, all builds get filtered out

**To fix:**
- Remove or adjust the `Parameter Run Matcher Filter` if it's too restrictive
- Or use `Parameter Build Filter Column` instead — it applies its own filter independently without requiring a view-level filter

> **Tip:** `DynamicBuildFilterColumn` + `Parameter Run Matcher Filter` is a global approach (affects all wrapped columns). `Parameter Build Filter Column` is a per-column approach (each column filters independently). Choose based on whether you want uniform or independent filtering.

## Dropdown values are empty

The Dropdown Filter View is a ListView — it only sees jobs that match the view's **Include jobs by regex** field. Make sure:
1. The **Include jobs by regex** field has a pattern broad enough to cover your jobs (e.g., `.*`)
2. **Recurse in subfolders** is enabled in the view configuration
3. The dropdown regex pattern has a capture group (e.g., `projects/([^/]+)/.*`)

## Regex Pattern Tips

The **Include jobs by regex** field and the **Job Name Regex** dropdown source work together but serve different purposes:

| Field | Purpose | Example |
|---|---|---|
| **Include jobs by regex** (view config) | Controls which jobs the view can see at all | `projects/.*` |
| **Job Name Regex** (dropdown source) | Extracts a capture group value for the dropdown | `projects/([^/]+)/.*` |

The include regex must be broad enough to cover all jobs you want the dropdowns to filter. The dropdown regex then extracts specific segments as dropdown values.
