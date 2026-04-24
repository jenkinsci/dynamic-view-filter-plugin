package io.jenkins.plugins.dynamic_view_filter;

import hudson.Extension;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.views.RunMatcher;
import hudson.views.ViewJobFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * A view-level job filter that implements {@link RunMatcher}.
 *
 * Add this ONCE to a view's Job Filters section, configure the parameter
 * name regex, value regex, and other matching options, and every
 * {@link DynamicBuildFilterColumn} in that view will automatically
 * filter builds through it.
 *
 * Matching behaviour mirrors the upstream ParameterFilter from
 * view-job-filters, including support for name regex, value regex,
 * description regex, default-value vs build-value matching,
 * multi-build scanning, in-progress matching, and include/exclude modes.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ParameterRunMatcherFilter extends ViewJobFilter implements RunMatcher {

    public enum IncludeExcludeType {
        includeMatched, includeUnmatched, excludeMatched, excludeUnmatched
    }

    private String includeExcludeTypeString;
    private transient IncludeExcludeType includeExcludeType;

    private String nameRegex;
    private transient Pattern namePattern;

    private String valueRegex;
    private transient Pattern valuePattern;

    private String descriptionRegex;
    private transient Pattern descriptionPattern;

    private Boolean useDefaultValue = Boolean.FALSE;
    private boolean matchBuildsInProgress = true;
    private boolean matchAllBuilds = true;
    private int maxBuildsToMatch;

    @DataBoundConstructor
    public ParameterRunMatcherFilter(String includeExcludeTypeString) {
        this.includeExcludeTypeString = includeExcludeTypeString;
        this.includeExcludeType = IncludeExcludeType.valueOf(includeExcludeTypeString);
    }

    Object readResolve() {
        if (nameRegex != null) {
            namePattern = ParameterUtils.toPattern(nameRegex);
        }
        if (valueRegex != null) {
            valuePattern = ParameterUtils.toPattern(valueRegex);
        }
        if (descriptionRegex != null) {
            descriptionPattern = ParameterUtils.toPattern(descriptionRegex);
        }
        if (useDefaultValue == null) {
            useDefaultValue = Boolean.TRUE;
        }
        if (includeExcludeTypeString != null) {
            includeExcludeType = IncludeExcludeType.valueOf(includeExcludeTypeString);
        }
        return this;
    }

    // ---- getters for Jelly binding ----

    public String getIncludeExcludeTypeString() {
        return includeExcludeTypeString;
    }

    public boolean isIncludeMatched() {
        return includeExcludeType == IncludeExcludeType.includeMatched;
    }

    public boolean isIncludeUnmatched() {
        return includeExcludeType == IncludeExcludeType.includeUnmatched;
    }

    public boolean isExcludeMatched() {
        return includeExcludeType == IncludeExcludeType.excludeMatched;
    }

    public boolean isExcludeUnmatched() {
        return includeExcludeType == IncludeExcludeType.excludeUnmatched;
    }

    public String getNameRegex() {
        return nameRegex;
    }

    @DataBoundSetter
    public void setNameRegex(String nameRegex) {
        this.nameRegex = nameRegex;
        this.namePattern = ParameterUtils.toPattern(nameRegex);
    }

    public String getValueRegex() {
        return valueRegex;
    }

    @DataBoundSetter
    public void setValueRegex(String valueRegex) {
        this.valueRegex = valueRegex;
        this.valuePattern = ParameterUtils.toPattern(valueRegex);
    }

    public String getDescriptionRegex() {
        return descriptionRegex;
    }

    @DataBoundSetter
    public void setDescriptionRegex(String descriptionRegex) {
        this.descriptionRegex = descriptionRegex;
        this.descriptionPattern = ParameterUtils.toPattern(descriptionRegex);
    }

    public boolean isUseDefaultValue() {
        return useDefaultValue;
    }

    @DataBoundSetter
    public void setUseDefaultValue(boolean useDefaultValue) {
        this.useDefaultValue = useDefaultValue;
    }

    public boolean isMatchAllBuilds() {
        return matchAllBuilds;
    }

    @DataBoundSetter
    public void setMatchAllBuilds(boolean matchAllBuilds) {
        this.matchAllBuilds = matchAllBuilds;
    }

    public int getMaxBuildsToMatch() {
        return maxBuildsToMatch;
    }

    @DataBoundSetter
    public void setMaxBuildsToMatch(int maxBuildsToMatch) {
        this.maxBuildsToMatch = maxBuildsToMatch;
    }

    public boolean isMatchBuildsInProgress() {
        return matchBuildsInProgress;
    }

    @DataBoundSetter
    public void setMatchBuildsInProgress(boolean matchBuildsInProgress) {
        this.matchBuildsInProgress = matchBuildsInProgress;
    }

    // ---- RunMatcher implementation (used by DynamicBuildFilterColumn) ----

    @Override
    public boolean matchesRun(Run run) {
        if (run == null) {
            return false;
        }
        ParametersAction action = run.getAction(ParametersAction.class);
        if (action == null) {
            return false;
        }
        for (ParameterValue pv : action.getParameters()) {
            String sval = ParameterUtils.getStringValue(pv);
            if (matchesParameter(pv.getName(), sval, false, null)) {
                return true;
            }
        }
        return false;
    }

    // ---- ViewJobFilter: include/exclude jobs from the view ----

    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
        List<TopLevelItem> filtered = new ArrayList<>(added);
        for (TopLevelItem item : all) {
            boolean matched = matchesItem(item);
            if (exclude(matched)) {
                filtered.remove(item);
            }
            if (include(matched) && !filtered.contains(item)) {
                filtered.add(item);
            }
        }
        List<TopLevelItem> sorted = new ArrayList<>(all);
        sorted.retainAll(filtered);
        return sorted;
    }

    private boolean include(boolean matched) {
        return (isIncludeMatched() && matched) || (isIncludeUnmatched() && !matched);
    }

    private boolean exclude(boolean matched) {
        return (isExcludeMatched() && matched) || (isExcludeUnmatched() && !matched);
    }

    private boolean matchesItem(TopLevelItem item) {
        if (!(item instanceof Job)) {
            return false;
        }
        Job job = (Job) item;
        if (useDefaultValue) {
            return matchesDefaultValue(job);
        } else {
            return matchesBuildValue(job);
        }
    }

    // ---- default value matching ----

    private boolean matchesDefaultValue(Job job) {
        ParametersDefinitionProperty property =
                (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);
        if (property == null) {
            return false;
        }
        for (ParameterDefinition def : property.getParameterDefinitions()) {
            boolean multiline = isValueMultiline(def);
            String svalue = getStringValue(def);
            if (matchesParameter(def.getName(), svalue, multiline, def.getDescription())) {
                return true;
            }
        }
        return false;
    }

    // ---- build value matching ----

    private boolean matchesBuildValue(Job job) {
        boolean matched = false;
        int count = 1;
        Run run = job.getLastBuild();
        while (run != null && !matched) {
            boolean isBuilding = run.isBuilding();
            if (matchBuildsInProgress || !isBuilding) {
                matched = matchesRun(run);
                if (!matchAllBuilds || (maxBuildsToMatch > 0 && count >= maxBuildsToMatch)) {
                    break;
                }
            }
            run = run.getPreviousBuild();
            count++;
        }
        return matched;
    }

    // ---- parameter matching ----

    private boolean matchesParameter(String name, String value, boolean isValueMultiline, String description) {
        if (!matchesPattern(namePattern, name, false)) {
            return false;
        }
        if (!matchesPattern(valuePattern, value, isValueMultiline)) {
            return false;
        }
        if (description != null && !matchesPattern(descriptionPattern, description, true)) {
            return false;
        }
        return true;
    }

    private static boolean matchesPattern(Pattern p, String m, boolean multiline) {
        if (p == null) {
            return true;
        }
        if (m == null) {
            return false;
        }
        Matcher matcher = p.matcher(m);
        return multiline ? matcher.find() : matcher.matches();
    }

    // ---- value extraction helpers ----

    private static String getStringValue(ParameterDefinition definition) {
        if (definition instanceof ChoiceParameterDefinition) {
            return ((ChoiceParameterDefinition) definition).getChoicesText();
        }
        ParameterValue value = definition.getDefaultParameterValue();
        return ParameterUtils.getStringValue(value);
    }

    private static boolean isValueMultiline(ParameterDefinition def) {
        return def instanceof ChoiceParameterDefinition;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return Messages.ParameterRunMatcherFilter_DisplayName();
        }
    }
}
