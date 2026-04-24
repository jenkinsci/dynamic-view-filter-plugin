package io.jenkins.plugins.dynamic_view_filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.BallColor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ListView;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;
import hudson.views.LastSuccessColumn;
import hudson.views.ParameterFilter;
import hudson.views.ViewJobFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DynamicBuildFilterColumnTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    // ---- helpers ----

    private WorkflowRun buildPipelineWithParam(WorkflowJob job, String paramName, String paramValue) throws Exception {
        QueueTaskFuture<WorkflowRun> f = job.scheduleBuild2(0,
                new ParametersAction(new StringParameterValue(paramName, paramValue)));
        assertNotNull("build should be scheduled", f);
        return j.assertBuildStatusSuccess(f);
    }

    private FreeStyleBuild buildFreeStyleWithParam(FreeStyleProject job, String paramName, String paramValue) throws Exception {
        QueueTaskFuture<FreeStyleBuild> f = job.scheduleBuild2(0,
                new ParametersAction(new StringParameterValue(paramName, paramValue)));
        assertNotNull("build should be scheduled", f);
        return j.assertBuildStatusSuccess(f);
    }

    private void setContextView(Object column, ListView view) throws Exception {
        Field f = column.getClass().getDeclaredField("contextView");
        f.setAccessible(true);
        f.set(column, view);
    }

    // ---- DynamicBuildFilterColumn tests (uses view RunMatcher filters) ----

    @Test
    public void filtersPipelineBuildsByViewRunMatcher() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline1");
        job.setDefinition(new CpsFlowDefinition("println 'ok'", true));
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west", "central"}, "region")));

        WorkflowRun runEast = buildPipelineWithParam(job, "region", "east");
        WorkflowRun runWest = buildPipelineWithParam(job, "region", "west");

        ListView view = new ListView("east-view", j.jenkins);
        j.jenkins.addView(view);
        view.getJobFilters().add(new ParameterFilter(
                "excludeUnmatched", "region", "east", "", false, true, 0, true));

        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        setContextView(column, view);

        hudson.model.Job wrapped = column.getJobWrapper(job);

        assertNotNull("last successful should exist", wrapped.getLastSuccessfulBuild());
        assertEquals(runEast.getNumber(), wrapped.getLastSuccessfulBuild().getNumber());
        assertNull("west build should be filtered out", wrapped.getBuildByNumber(runWest.getNumber()));
    }

    @Test
    public void filtersFreeStyleBuildsByViewRunMatcher() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("freestyle1");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        FreeStyleBuild runEast = buildFreeStyleWithParam(job, "region", "east");
        FreeStyleBuild runWest = buildFreeStyleWithParam(job, "region", "west");

        ListView view = new ListView("east-view", j.jenkins);
        j.jenkins.addView(view);
        view.getJobFilters().add(new ParameterFilter(
                "excludeUnmatched", "region", "east", "", false, true, 0, true));

        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        setContextView(column, view);

        hudson.model.Job wrapped = column.getJobWrapper(job);

        assertNotNull("last successful should exist", wrapped.getLastSuccessfulBuild());
        assertEquals(runEast.getNumber(), wrapped.getLastSuccessfulBuild().getNumber());
        assertNull("west build should be filtered out", wrapped.getBuildByNumber(runWest.getNumber()));
    }

    // ---- ParameterBuildFilterColumn tests (self-contained param filter) ----

    @Test
    public void paramColumnFiltersPipelineBuilds() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline2");
        job.setDefinition(new CpsFlowDefinition("println 'ok'", true));
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west", "central"}, "region")));

        WorkflowRun runEast = buildPipelineWithParam(job, "region", "east");
        WorkflowRun runWest = buildPipelineWithParam(job, "region", "west");
        WorkflowRun runCentral = buildPipelineWithParam(job, "region", "central");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);

        assertNotNull("last build should exist", wrapped.getLastBuild());
        assertEquals(runEast.getNumber(), wrapped.getLastBuild().getNumber());
        assertNull("west build should be filtered", wrapped.getBuildByNumber(runWest.getNumber()));
        assertNull("central build should be filtered", wrapped.getBuildByNumber(runCentral.getNumber()));
    }

    @Test
    public void paramColumnFiltersFreeStyleBuilds() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("freestyle2");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        FreeStyleBuild runEast = buildFreeStyleWithParam(job, "region", "east");
        FreeStyleBuild runWest = buildFreeStyleWithParam(job, "region", "west");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);

        assertNotNull("last build should exist", wrapped.getLastBuild());
        assertEquals(runEast.getNumber(), wrapped.getLastBuild().getNumber());
        assertNull("west build should be filtered", wrapped.getBuildByNumber(runWest.getNumber()));
    }

    @Test
    public void paramColumnSupportsRegex() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline3");
        job.setDefinition(new CpsFlowDefinition("println 'ok'", true));
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west", "central"}, "region")));

        WorkflowRun runEast = buildPipelineWithParam(job, "region", "east");
        WorkflowRun runWest = buildPipelineWithParam(job, "region", "west");
        WorkflowRun runCentral = buildPipelineWithParam(job, "region", "central");

        // regex matches east or central
        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east|central");

        hudson.model.Job wrapped = column.getJobWrapper(job);

        assertNotNull("central build should match", wrapped.getBuildByNumber(runCentral.getNumber()));
        assertNotNull("east build should match", wrapped.getBuildByNumber(runEast.getNumber()));
        assertNull("west build should not match", wrapped.getBuildByNumber(runWest.getNumber()));
        assertEquals(2, wrapped.getBuilds().size());
    }

    // ---- ParameterRunMatcherFilter tests (view-level filter) ----

    @Test
    public void runMatcherFilterUsesNameRegex() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline4");
        job.setDefinition(new CpsFlowDefinition("println 'ok'", true));
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        WorkflowRun runEast = buildPipelineWithParam(job, "region", "east");
        WorkflowRun runWest = buildPipelineWithParam(job, "region", "west");

        ListView view = new ListView("test-rmf", j.jenkins);
        j.jenkins.addView(view);
        // nameRegex matches "region", value matches "east", scan all builds
        ParameterRunMatcherFilter rmf1 = new ParameterRunMatcherFilter("includeMatched");
        rmf1.setNameRegex("reg.*");
        rmf1.setValueRegex("east");
        view.getJobFilters().add(rmf1);

        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        setContextView(column, view);

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("east build should match", wrapped.getBuildByNumber(runEast.getNumber()));
        assertNull("west build should not match", wrapped.getBuildByNumber(runWest.getNumber()));
    }

    @Test
    public void paramColumnNameRegexMatches() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline5");
        job.setDefinition(new CpsFlowDefinition("println 'ok'", true));
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("deploy_target", new String[]{"alpha", "beta"}, "target")));

        WorkflowRun runAlpha = buildPipelineWithParam(job, "deploy_target", "alpha");
        WorkflowRun runBeta = buildPipelineWithParam(job, "deploy_target", "beta");

        // name regex matches "deploy.*" (matches "deploy_target"), value matches "alpha"
        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "deploy.*", "alpha");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("alpha build should match", wrapped.getBuildByNumber(runAlpha.getNumber()));
        assertNull("beta build should not match", wrapped.getBuildByNumber(runBeta.getNumber()));
    }

    // ---- edge case tests ----

    @Test
    public void filteredJobWithNoBuildsReturnsNull() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("no-builds");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNull("no builds means null last build", wrapped.getLastBuild());
        assertNull("no builds means null first build", wrapped.getFirstBuild());
        assertNull("no builds means null last successful", wrapped.getLastSuccessfulBuild());
        assertNull("no builds means null last failed", wrapped.getLastFailedBuild());
        assertEquals(0, wrapped.getBuilds().size());
    }

    @Test
    public void filteredJobWithNoMatchingBuilds() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("no-match");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        buildFreeStyleWithParam(job, "region", "east");
        buildFreeStyleWithParam(job, "region", "east");

        // filter for "central" which no build has
        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "central");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNull("no matching builds", wrapped.getLastBuild());
        assertEquals(0, wrapped.getBuilds().size());
    }

    @Test
    public void runMatcherFilterPassesMatchingBuildsToColumn() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline-rmf2");
        job.setDefinition(new CpsFlowDefinition("println 'ok'", true));
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        WorkflowRun runEast = buildPipelineWithParam(job, "region", "east");
        WorkflowRun runWest = buildPipelineWithParam(job, "region", "west");

        ListView view = new ListView("rmf-test", j.jenkins);
        j.jenkins.addView(view);
        // RunMatcher matches region=east; column only shows matching builds
        ParameterRunMatcherFilter rmf2 = new ParameterRunMatcherFilter("includeMatched");
        rmf2.setNameRegex("region");
        rmf2.setValueRegex("east");
        view.getJobFilters().add(rmf2);

        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        setContextView(column, view);

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("east matches RunMatcher", wrapped.getBuildByNumber(runEast.getNumber()));
        assertNull("west does not match RunMatcher", wrapped.getBuildByNumber(runWest.getNumber()));
        assertEquals("only 1 build visible", 1, wrapped.getBuilds().size());
    }

    // ---- ParameterRunMatcherFilter ViewJobFilter / setter coverage tests ----

    @Test
    public void runMatcherFilterDefaultValueIncludesJob() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("filter-default");
        // Default value of ChoiceParameterDefinition is first choice ("east")
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "choose region")));

        ListView view = new ListView("filter-view", j.jenkins);
        j.jenkins.addView(view);

        ParameterRunMatcherFilter rmf = new ParameterRunMatcherFilter("includeMatched");
        rmf.setNameRegex("region");
        rmf.setValueRegex("east");
        rmf.setDescriptionRegex("choose.*");
        rmf.setUseDefaultValue(true);

        // Verify setters via getters (covers getter lines)
        assertEquals("choose.*", rmf.getDescriptionRegex());
        assertTrue(rmf.isUseDefaultValue());

        List<TopLevelItem> added = new ArrayList<>();
        List<TopLevelItem> all = new ArrayList<>();
        all.add(job);

        List<TopLevelItem> result = rmf.filter(added, all, view);
        assertEquals("job should be included by default value match", 1, result.size());
    }

    @Test
    public void runMatcherFilterBuildValueWithMaxBuilds() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("filter-builds");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        buildFreeStyleWithParam(job, "region", "west");
        buildFreeStyleWithParam(job, "region", "east");

        ListView view = new ListView("filter-view2", j.jenkins);
        j.jenkins.addView(view);

        ParameterRunMatcherFilter rmf = new ParameterRunMatcherFilter("includeMatched");
        rmf.setNameRegex("region");
        rmf.setValueRegex("east");
        rmf.setUseDefaultValue(false);
        rmf.setMatchAllBuilds(true);
        rmf.setMaxBuildsToMatch(1);
        rmf.setMatchBuildsInProgress(true);

        // Verify setters via getters (covers getter lines)
        assertFalse(rmf.isUseDefaultValue());
        assertTrue(rmf.isMatchAllBuilds());
        assertEquals(1, rmf.getMaxBuildsToMatch());
        assertTrue(rmf.isMatchBuildsInProgress());

        List<TopLevelItem> added = new ArrayList<>();
        List<TopLevelItem> all = new ArrayList<>();
        all.add(job);

        // last build is "east", maxBuildsToMatch=1 so only checks last build → matches
        List<TopLevelItem> result = rmf.filter(added, all, view);
        assertEquals("job should be included (last build matches)", 1, result.size());
    }

    @Test
    public void runMatcherFilterExcludeMatchedMode() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("filter-exclude");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        buildFreeStyleWithParam(job, "region", "east");

        ListView view = new ListView("filter-view3", j.jenkins);
        j.jenkins.addView(view);

        ParameterRunMatcherFilter rmf = new ParameterRunMatcherFilter("excludeMatched");
        rmf.setNameRegex("region");
        rmf.setValueRegex("east");
        rmf.setUseDefaultValue(false);
        rmf.setMatchAllBuilds(false);
        rmf.setMatchBuildsInProgress(false);

        // Verify setter values
        assertFalse(rmf.isMatchBuildsInProgress());
        assertFalse(rmf.isMatchAllBuilds());

        List<TopLevelItem> added = new ArrayList<>();
        added.add(job);
        List<TopLevelItem> all = new ArrayList<>();
        all.add(job);

        // job matches (build has region=east), excludeMatched removes it
        List<TopLevelItem> result = rmf.filter(added, all, view);
        assertEquals("matched job should be excluded", 0, result.size());
    }

    @Test
    public void runMatcherFilterDefaultValueWithStringParam() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("filter-string-default");
        job.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("env", "production", "deployment environment")));

        ListView view = new ListView("filter-view4", j.jenkins);
        j.jenkins.addView(view);

        ParameterRunMatcherFilter rmf = new ParameterRunMatcherFilter("includeMatched");
        rmf.setNameRegex("env");
        rmf.setValueRegex("production");
        rmf.setDescriptionRegex("deployment.*");
        rmf.setUseDefaultValue(true);
        rmf.setMaxBuildsToMatch(5);

        assertEquals(5, rmf.getMaxBuildsToMatch());

        List<TopLevelItem> added = new ArrayList<>();
        List<TopLevelItem> all = new ArrayList<>();
        all.add(job);

        List<TopLevelItem> result = rmf.filter(added, all, view);
        assertEquals("job with matching string default should be included", 1, result.size());
    }

    // ---- FilteredJob additional coverage tests ----

    @Test
    public void filteredJobDelegatesDescriptionAndBuildable() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("delegate-test");
        job.setDescription("test description");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertEquals("test description", wrapped.getDescription());
        assertTrue("should be buildable", wrapped.isBuildable());
        assertNotNull("should have real job descriptor", ((FilteredJob) wrapped).getDescriptor());
        assertEquals(job, ((FilteredJob) wrapped).getRealJob());
    }

    @Test
    public void filteredJobIconColorNoBuilds() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("icon-test");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertEquals(BallColor.NOTBUILT, wrapped.getIconColor());
    }

    @Test
    public void filteredJobIconColorWithBuilds() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("icon-with-builds");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east"}, "region")));

        buildFreeStyleWithParam(job, "region", "east");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("icon color should not be null", wrapped.getIconColor());
        // successful build → blue
        assertEquals(BallColor.BLUE, wrapped.getIconColor());
    }

    @Test
    public void filteredJobLastCompletedAndUnstableBuild() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("completed-test");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east"}, "region")));

        FreeStyleBuild build = buildFreeStyleWithParam(job, "region", "east");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("last completed build should exist", wrapped.getLastCompletedBuild());
        assertEquals(build.getNumber(), wrapped.getLastCompletedBuild().getNumber());
        assertNull("no unstable builds", wrapped.getLastUnstableBuild());
    }

    @Test
    public void filteredJobBuildHealthReportsEmpty() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("health-empty");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertTrue("no builds means empty health reports", wrapped.getBuildHealthReports().isEmpty());
    }

    @Test
    public void filteredJobBuildHealthReportsWithBuilds() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("health-with-builds");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east"}, "region")));

        buildFreeStyleWithParam(job, "region", "east");

        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east");

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("health reports should not be null", wrapped.getBuildHealthReports());
    }

    // ---- DynamicBuildFilterColumn additional coverage ----

    @Test
    public void dynamicColumnGetColumnCaption() throws Exception {
        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        assertNotNull("column caption should exist", column.getColumnCaption());

        DynamicBuildFilterColumn nullDelegateCol = new DynamicBuildFilterColumn(null);
        assertNotNull("null delegate caption should not be null", nullDelegateCol.getColumnCaption());
    }

    @Test
    public void dynamicColumnGetAllColumnsExcludesFilterColumns() throws Exception {
        List<?> columns = DynamicBuildFilterColumn.getAllColumns();
        assertNotNull("column list should not be null", columns);
        for (Object desc : columns) {
            assertFalse("should not contain DynamicBuildFilterColumn",
                    desc instanceof DynamicBuildFilterColumn.DescriptorImpl);
            assertFalse("should not contain ParameterBuildFilterColumn",
                    desc instanceof ParameterBuildFilterColumn.DescriptorImpl);
        }
    }

    @Test
    public void dynamicColumnWithoutViewReturnsUnfilteredJob() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("no-view-job");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east"}, "region")));

        buildFreeStyleWithParam(job, "region", "east");

        // Column without contextView set — should return the job itself unfiltered
        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("should return a job even without view", wrapped);
    }

    // ---- ParameterBuildFilterColumn additional coverage ----

    @Test
    public void paramColumnGettersReturnConfiguredValues() throws Exception {
        ParameterBuildFilterColumn column = new ParameterBuildFilterColumn(
                new LastSuccessColumn(), "region", "east|west");
        assertEquals("region", column.getParamName());
        assertEquals("east|west", column.getParamValueRegex());
        assertNotNull("delegate should not be null", column.getDelegate());
        assertNotNull("column caption should exist", column.getColumnCaption());
    }

    // ---- ParameterRunMatcherFilter include/exclude type getters ----

    @Test
    public void runMatcherFilterIncludeExcludeTypeChecks() throws Exception {
        ParameterRunMatcherFilter rmfIncMatched = new ParameterRunMatcherFilter("includeMatched");
        assertTrue(rmfIncMatched.isIncludeMatched());
        assertFalse(rmfIncMatched.isIncludeUnmatched());
        assertFalse(rmfIncMatched.isExcludeMatched());
        assertFalse(rmfIncMatched.isExcludeUnmatched());
        assertEquals("includeMatched", rmfIncMatched.getIncludeExcludeTypeString());

        ParameterRunMatcherFilter rmfIncUnmatched = new ParameterRunMatcherFilter("includeUnmatched");
        assertFalse(rmfIncUnmatched.isIncludeMatched());
        assertTrue(rmfIncUnmatched.isIncludeUnmatched());

        ParameterRunMatcherFilter rmfExcMatched = new ParameterRunMatcherFilter("excludeMatched");
        assertTrue(rmfExcMatched.isExcludeMatched());
        assertFalse(rmfExcMatched.isExcludeUnmatched());

        ParameterRunMatcherFilter rmfExcUnmatched = new ParameterRunMatcherFilter("excludeUnmatched");
        assertFalse(rmfExcUnmatched.isExcludeMatched());
        assertTrue(rmfExcUnmatched.isExcludeUnmatched());
    }

    @Test
    public void runMatcherFilterMatchesRunNullReturnsfalse() throws Exception {
        ParameterRunMatcherFilter rmf = new ParameterRunMatcherFilter("includeMatched");
        rmf.setNameRegex("region");
        rmf.setValueRegex("east");
        assertFalse("null run should not match", rmf.matchesRun(null));
    }

    @Test
    public void runMatcherFilterMatchesRunWithNoParams() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("no-params-run");
        FreeStyleBuild build = j.buildAndAssertSuccess(job);

        ParameterRunMatcherFilter rmf = new ParameterRunMatcherFilter("includeMatched");
        rmf.setNameRegex("region");
        assertFalse("build without parameters should not match", rmf.matchesRun(build));
    }
}
