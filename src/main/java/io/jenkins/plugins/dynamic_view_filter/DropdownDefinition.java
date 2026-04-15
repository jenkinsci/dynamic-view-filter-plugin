package io.jenkins.plugins.dynamic_view_filter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.util.ListBoxModel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Defines a single dropdown filter for {@link DropdownFilterView}.
 *
 * <p>Two source types are supported:
 * <ul>
 *   <li><b>jobNameRegex</b> — a regex with a capture group applied to the
 *       job's full name. The first capture group's match becomes the dropdown
 *       value. Example: {@code projects/([^/]+)/.*} captures the project name.</li>
 *   <li><b>buildParameter</b> — collects distinct values of a named build
 *       parameter from actual build runs.</li>
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DropdownDefinition extends AbstractDescribableImpl<DropdownDefinition> {

    private final String label;
    private final String sourceType;
    private final String jobNamePattern;
    private final String parameterName;
    private transient Pattern compiledJobNamePattern;

    // Legacy field kept for backward-compatible XML deserialization.
    // Old configs stored pathSegmentIndex; we silently ignore it.
    @SuppressWarnings("unused")
    private transient int pathSegmentIndex;

    @DataBoundConstructor
    public DropdownDefinition(String label, String sourceType, String jobNamePattern, String parameterName) {
        this.label = label;
        this.sourceType = sourceType;
        this.jobNamePattern = jobNamePattern != null ? jobNamePattern : "";
        this.parameterName = parameterName != null ? parameterName : "";
        this.compiledJobNamePattern = ParameterUtils.toPattern(this.jobNamePattern);
    }

    Object readResolve() {
        compiledJobNamePattern = ParameterUtils.toPattern(jobNamePattern);
        return this;
    }

    public String getLabel() {
        return label;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getJobNamePattern() {
        return jobNamePattern;
    }

    public String getParameterName() {
        return parameterName;
    }

    /**
     * Extract all possible values for this dropdown from the given item.
     * For jobNameRegex: applies the regex to the job's full name and extracts
     * the first capture group.
     * For buildParameter: scans actual build runs to collect distinct values.
     */
    public List<String> extractAllValues(TopLevelItem item) {
        List<String> values = new ArrayList<>();
        if ("jobNameRegex".equals(sourceType)) {
            String val = extractCaptureGroup(item);
            if (val != null && !val.isEmpty()) {
                values.add(val);
            }
        } else if ("buildParameter".equals(sourceType) && item instanceof Job) {
            Job job = (Job) item;
            Run run = job.getLastBuild();
            while (run != null) {
                String val = ParameterUtils.getParamValue(run, parameterName);
                if (val != null && !values.contains(val)) {
                    values.add(val);
                }
                run = run.getPreviousBuild();
            }
        }
        return values;
    }

    /**
     * Check if an item matches the given dropdown selection.
     */
    public boolean matches(TopLevelItem item, String selected) {
        if (selected == null || selected.isEmpty()) {
            return true;
        }
        if ("jobNameRegex".equals(sourceType)) {
            String val = extractCaptureGroup(item);
            return selected.equals(val);
        } else if ("buildParameter".equals(sourceType) && item instanceof Job) {
            Job job = (Job) item;
            Run run = job.getLastBuild();
            while (run != null) {
                String val = ParameterUtils.getParamValue(run, parameterName);
                if (selected.equals(val)) {
                    return true;
                }
                run = run.getPreviousBuild();
            }
            return false;
        }
        return true;
    }

    private String extractCaptureGroup(TopLevelItem item) {
        if (compiledJobNamePattern == null) {
            return null;
        }
        Matcher m = compiledJobNamePattern.matcher(item.getFullName());
        if (m.matches() && m.groupCount() >= 1) {
            return m.group(1);
        }
        // Try find() for partial matches
        m.reset();
        if (m.find() && m.groupCount() >= 1) {
            return m.group(1);
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DropdownDefinition> {
        @Override
        public String getDisplayName() {
            return "Dropdown Filter";
        }

        public ListBoxModel doFillSourceTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Job Name Regex", "jobNameRegex");
            items.add("Build Parameter", "buildParameter");
            return items;
        }
    }
}
