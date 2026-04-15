package io.jenkins.plugins.dynamic_view_filter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.RunMatcher;
import hudson.views.ViewJobFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Pipeline-safe build filter wrapper column.
 *
 * Resolves the parent {@link ListView} at render time from Stapler request
 * context (no stored XStream back-reference needed), and filters build data
 * through the view's {@link RunMatcher} job filters before the delegate
 * column renders.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DynamicBuildFilterColumn extends ListViewColumn {

    private static final Logger LOGGER = Logger.getLogger(DynamicBuildFilterColumn.class.getName());

    private final ListViewColumn delegate;
    private transient ListView contextView;

    @DataBoundConstructor
    public DynamicBuildFilterColumn(ListViewColumn delegate) {
        this.delegate = delegate;
    }

    public ListViewColumn getDelegate() {
        return delegate;
    }

    @Override
    public String getColumnCaption() {
        return delegate != null ? delegate.getColumnCaption() : super.getColumnCaption();
    }

    // ---- static helpers used by config.jelly ----

    public static List<Descriptor<ListViewColumn>> doGetAllColumns() {
        DescriptorExtensionList<ListViewColumn, Descriptor<ListViewColumn>> all = ListViewColumn.all();
        List<Descriptor<ListViewColumn>> list = new ArrayList<>();
        for (Descriptor<ListViewColumn> descriptor : all) {
            String clsName = descriptor.clazz.getName();
            if (descriptor instanceof DescriptorImpl
                    || descriptor instanceof ParameterBuildFilterColumn.DescriptorImpl
                    || clsName.contains("BuildFilterColumn")) {
                continue;
            }
            list.add(descriptor);
        }
        return list;
    }

    // ---- column.jelly entry point ----

    public Job getJobWrapper(Job job) {
        ListView view = resolveCurrentView();
        if (view == null) {
            LOGGER.fine(() -> "No ListView context for " + job.getFullName() + "; returning unfiltered");
            return job;
        }
        return new FilteredJob(job, run -> matchesRun(view, run));
    }

    // ---- view resolution ----

    private @CheckForNull ListView resolveCurrentView() {
        try {
            StaplerRequest2 req = Stapler.getCurrentRequest2();
            if (req != null) {
                ListView current = req.findAncestorObject(ListView.class);
                if (current != null) {
                    contextView = current;
                    return current;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error resolving Stapler request context", e);
        }
        return contextView;
    }

    // ---- run matching ----

    private static List<ViewJobFilter> safeFilters(ListView view) {
        List<ViewJobFilter> filters = view.getJobFilters();
        return filters == null ? Collections.emptyList() : filters;
    }

    static boolean matchesRun(ListView view, Run run) {
        if (run == null) {
            return false;
        }
        for (ViewJobFilter filter : safeFilters(view)) {
            if (filter instanceof RunMatcher && !((RunMatcher) filter).matchesRun(run)) {
                return false;
            }
        }
        if (view instanceof DropdownFilterView) {
            for (RunMatcher matcher : ((DropdownFilterView) view).getActiveRunMatchers()) {
                if (!matcher.matchesRun(run)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- descriptor ----

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DynamicBuildFilterColumn_DisplayName();
        }

        @Override
        public ListViewColumn newInstance(StaplerRequest2 req, net.sf.json.JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            DynamicBuildFilterColumn col = (DynamicBuildFilterColumn) super.newInstance(req, formData);
            if (req != null) {
                ListView listView = req.findAncestorObject(ListView.class);
                if (listView != null) {
                    col.contextView = listView;
                } else {
                    LOGGER.fine("DynamicBuildFilterColumn configured outside ListView context");
                }
            }
            return col;
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }
    }
}
