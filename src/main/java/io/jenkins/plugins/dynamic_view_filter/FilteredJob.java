package io.jenkins.plugins.dynamic_view_filter;

import hudson.model.BallColor;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.util.RunList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * A thin Job wrapper that intercepts all build-data accessors and filters
 * them through a supplied {@link Predicate}. Works for any job type
 * (FreeStyle, Pipeline/WorkflowJob, Matrix, etc.) because it delegates
 * to the real job's builds and only filters at the {@link Run} level.
 *
 * <p>Shared by {@link DynamicBuildFilterColumn} and
 * {@link ParameterBuildFilterColumn} to avoid duplicating the ~120 lines
 * of build-accessor overrides.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FilteredJob extends Job implements TopLevelItem {

    private final Job delegateJob;
    private final Predicate<Run> runFilter;

    public FilteredJob(Job delegateJob, Predicate<Run> runFilter) {
        super(delegateJob.getParent(), delegateJob.getName());
        this.delegateJob = delegateJob;
        this.runFilter = runFilter;
    }

    public Job getRealJob() {
        return delegateJob;
    }

    // ---- build accessors (filtered) ----

    @Override
    public Run getLastBuild() {
        List<Run> runs = matchingRunsNewestFirst();
        return runs.isEmpty() ? null : runs.get(0);
    }

    @Override
    public RunList getBuilds() {
        return RunList.fromRuns(matchingRunsNewestFirst());
    }

    @Override
    protected SortedMap<Integer, Run> _getRuns() {
        SortedMap<Integer, Run> runs = new TreeMap<>();
        for (Run run : matchingRunsNewestFirst()) {
            runs.put(run.getNumber(), run);
        }
        return runs;
    }

    @Override
    public Run getFirstBuild() {
        List<Run> runs = matchingRunsNewestFirst();
        return runs.isEmpty() ? null : runs.get(runs.size() - 1);
    }

    @Override
    public Run getLastCompletedBuild() {
        for (Run run : matchingRunsNewestFirst()) {
            if (!run.isBuilding()) {
                return run;
            }
        }
        return null;
    }

    @Override
    public Run getLastSuccessfulBuild() {
        return firstWithResult(Result.SUCCESS);
    }

    @Override
    public Run getLastFailedBuild() {
        return firstWithResult(Result.FAILURE);
    }

    @Override
    public Run getLastUnstableBuild() {
        return firstWithResult(Result.UNSTABLE);
    }

    @Override
    public Run getBuildByNumber(int n) {
        Run run = delegateJob.getBuildByNumber(n);
        if (run == null) {
            return null;
        }
        return runFilter.test(run) ? run : null;
    }

    // ---- delegated state ----

    @Override
    public BallColor getIconColor() {
        Run last = getLastBuild();
        return last != null ? last.getIconColor() : BallColor.NOTBUILT;
    }

    @Override
    public boolean isBuildable() {
        return delegateJob.isBuildable();
    }

    @Override
    protected void removeRun(Run run) {
        // read-only wrapper
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return ((TopLevelItem) delegateJob).getDescriptor();
    }

    @Override
    public List<HealthReport> getBuildHealthReports() {
        if (matchingRunsNewestFirst().isEmpty()) {
            return Collections.emptyList();
        }
        return super.getBuildHealthReports();
    }

    // ---- private helpers ----

    private Run firstWithResult(Result result) {
        for (Run run : matchingRunsNewestFirst()) {
            if (result.equals(run.getResult())) {
                return run;
            }
        }
        return null;
    }

    private List<Run> matchingRunsNewestFirst() {
        List<Run> runs = new ArrayList<>();
        for (Object value : delegateJob.getBuilds()) {
            Run run = (Run) value;
            if (runFilter.test(run)) {
                runs.add(run);
            }
        }
        runs.sort(Comparator.comparingInt((Run r) -> r.getNumber()).reversed());
        return runs;
    }
}
