package io.jenkins.plugins.dynamic_view_filter;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A self-contained column wrapper that filters build data by a specific build
 * parameter name/value regex. Unlike {@link DynamicBuildFilterColumn}, this
 * column does NOT require RunMatcher filters in the view — the parameter
 * matching is configured directly on the column itself.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ParameterBuildFilterColumn extends ListViewColumn {

    private static final Logger LOGGER = Logger.getLogger(ParameterBuildFilterColumn.class.getName());

    private final ListViewColumn delegate;
    private final String paramName;
    private final String paramValueRegex;
    private transient Pattern namePattern;
    private transient Pattern valuePattern;

    @DataBoundConstructor
    public ParameterBuildFilterColumn(ListViewColumn delegate, String paramName, String paramValueRegex) {
        this.delegate = delegate;
        this.paramName = paramName;
        this.paramValueRegex = paramValueRegex;
        this.namePattern = ParameterUtils.toPattern(paramName);
        this.valuePattern = ParameterUtils.toPattern(paramValueRegex);
    }

    Object readResolve() {
        namePattern = ParameterUtils.toPattern(paramName);
        valuePattern = ParameterUtils.toPattern(paramValueRegex);
        return this;
    }

    public ListViewColumn getDelegate() {
        return delegate;
    }

    @Override
    public String getColumnCaption() {
        return delegate != null ? delegate.getColumnCaption() : super.getColumnCaption();
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamValueRegex() {
        return paramValueRegex;
    }

    public Job getJobWrapper(Job job) {
        return new FilteredJob(job, this::matchesRun);
    }

    private boolean matchesRun(Run run) {
        if (run == null) {
            return false;
        }
        ParametersAction action = run.getAction(ParametersAction.class);
        if (action == null) {
            return false;
        }
        for (ParameterValue pv : action.getParameters()) {
            if (ParameterUtils.matchesPattern(namePattern, pv.getName())) {
                String val = ParameterUtils.getStringValue(pv);
                if (ParameterUtils.matchesPattern(valuePattern, val)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Descriptor<ListViewColumn>> getAllColumns() {
        return DynamicBuildFilterColumn.getAllColumns();
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ParameterBuildFilterColumn_DisplayName();
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }
    }
}
