package io.jenkins.plugins.dynamic_view_filter;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.views.RunMatcher;
import hudson.views.ViewJobFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jakarta.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * A ListView extension that adds configurable dropdown filters at the top
 * of the view. Each dropdown can filter jobs by a path segment of the job's
 * full name or by a build parameter value.
 *
 * <p>Dropdown values are auto-discovered from the jobs in the view.
 * Selecting a value in a dropdown narrows the displayed jobs. Multiple
 * dropdowns combine with AND logic.
 *
 * <p>All standard ListView features (columns, job filters, regex) are
 * preserved.
 */
public class DropdownFilterView extends ListView {

    private List<DropdownDefinition> dropdowns = new ArrayList<>();
    private String filterPosition = "top";

    @DataBoundConstructor
    public DropdownFilterView(String name) {
        super(name);
    }

    public DropdownFilterView(String name, ViewGroup owner) {
        super(name, owner);
    }

    public List<DropdownDefinition> getDropdowns() {
        return dropdowns != null ? dropdowns : new ArrayList<>();
    }

    public void setDropdowns(List<DropdownDefinition> dropdowns) {
        this.dropdowns = dropdowns;
    }

    public String getFilterPosition() {
        return filterPosition != null ? filterPosition : "top";
    }

    public void setFilterPosition(String filterPosition) {
        this.filterPosition = filterPosition;
    }

    // ---- item filtering based on dropdown selections ----

    @Override
    public List<TopLevelItem> getItems() {
        List<TopLevelItem> items = super.getItems();
        List<DropdownDefinition> dds = getDropdowns();
        // Exclude non-Job items (e.g. Folders) that render as empty rows
        if (!dds.isEmpty()) {
            List<TopLevelItem> jobItems = new ArrayList<>();
            for (TopLevelItem item : items) {
                if (item instanceof Job) {
                    jobItems.add(item);
                }
            }
            items = jobItems;
        }
        Map<Integer, String> selections = getSelectionsFromRequest();
        if (selections.isEmpty()) {
            return items;
        }
        List<TopLevelItem> filtered = new ArrayList<>();
        for (TopLevelItem item : items) {
            boolean match = true;
            for (Map.Entry<Integer, String> entry : selections.entrySet()) {
                int idx = entry.getKey();
                String selected = entry.getValue();
                if (idx >= 0 && idx < dds.size()) {
                    if (!dds.get(idx).matches(item, selected)) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    // ---- dropdown value discovery (uses unfiltered item list) ----

    /**
     * Called from main.jelly to populate dropdown options.
     * Uses the unfiltered list so all values are always visible.
     */
    public List<String> getDropdownValues(DropdownDefinition dd) {
        List<TopLevelItem> items = super.getItems();
        Set<String> values = new TreeSet<>();
        for (TopLevelItem item : items) {
            values.addAll(dd.extractAllValues(item));
        }
        return new ArrayList<>(values);
    }

    /**
     * Called from main.jelly to mark the currently selected option.
     */
    public String getSelectedValue(int index) {
        try {
            StaplerRequest2 req = Stapler.getCurrentRequest2();
            if (req != null) {
                String val = req.getParameter("dd_" + index);
                return val != null ? val : "";
            }
        } catch (Exception e) {
            // not in a request context
        }
        return "";
    }

    // ---- request parsing ----

    private Map<Integer, String> getSelectionsFromRequest() {
        Map<Integer, String> selections = new LinkedHashMap<>();
        try {
            StaplerRequest2 req = Stapler.getCurrentRequest2();
            if (req != null) {
                List<DropdownDefinition> dds = getDropdowns();
                for (int i = 0; i < dds.size(); i++) {
                    String val = req.getParameter("dd_" + i);
                    if (val != null && !val.isEmpty()) {
                        selections.put(i, val);
                    }
                }
            }
        } catch (Exception e) {
            // not in a request context
        }
        return selections;
    }

    // ---- view configuration save ----

    @Override
    protected void submit(StaplerRequest2 req)
            throws IOException, ServletException, Descriptor.FormException {
        super.submit(req);
        JSONObject form = req.getSubmittedForm();
        Object raw = form.opt("dropdowns");
        if (raw != null) {
            dropdowns = req.bindJSONToList(DropdownDefinition.class, raw);
        } else {
            dropdowns = new ArrayList<>();
        }
        String pos = form.optString("filterPosition", "top");
        filterPosition = ("sidebar".equals(pos)) ? "sidebar" : "top";
    }

    // ---- dynamic RunMatchers from build parameter dropdown selections ----

    /**
     * Returns dynamic RunMatcher filters derived from active build-parameter
     * dropdown selections. Called by DynamicBuildFilterColumn to filter
     * displayed builds based on dropdown selections.
     */
    public List<RunMatcher> getActiveRunMatchers() {
        List<RunMatcher> matchers = new ArrayList<>();
        Map<Integer, String> selections = getSelectionsFromRequest();
        if (selections.isEmpty()) {
            return matchers;
        }
        List<DropdownDefinition> dds = getDropdowns();
        for (Map.Entry<Integer, String> entry : selections.entrySet()) {
            int idx = entry.getKey();
            String selected = entry.getValue();
            if (idx >= 0 && idx < dds.size()) {
                DropdownDefinition dd = dds.get(idx);
                if ("buildParameter".equals(dd.getSourceType())
                        && selected != null && !selected.isEmpty()) {
                    matchers.add(new DropdownRunMatcher(dd.getParameterName(), selected));
                }
            }
        }
        return matchers;
    }

    /**
     * A lightweight RunMatcher that checks if a build has a specific
     * parameter value. Created dynamically when a build-parameter
     * dropdown has an active selection.
     */
    @SuppressWarnings("rawtypes")
    static class DropdownRunMatcher implements RunMatcher {
        private final String paramName;
        private final String paramValue;

        DropdownRunMatcher(String paramName, String paramValue) {
            this.paramName = paramName;
            this.paramValue = paramValue;
        }

        @Override
        public boolean matchesRun(Run run) {
            if (run == null) {
                return false;
            }
            String val = ParameterUtils.getParamValue(run, paramName);
            return paramValue.equals(val);
        }
    }

    @Extension
    public static class DescriptorImpl extends ViewDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DropdownFilterView_DisplayName();
        }
    }
}
