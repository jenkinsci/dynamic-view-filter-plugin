package io.jenkins.plugins.dynamic_view_filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ListView;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import hudson.views.LastSuccessColumn;
import hudson.views.ParameterFilter;
import hudson.views.ViewJobFilter;
import java.lang.reflect.Field;
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
        view.getJobFilters().add(new ParameterRunMatcherFilter(
                "includeMatched", "reg.*", "east", "", false, true, 0, true));

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
        view.getJobFilters().add(new ParameterRunMatcherFilter(
                "includeMatched", "region", "east", "", false, true, 0, true));

        DynamicBuildFilterColumn column = new DynamicBuildFilterColumn(new LastSuccessColumn());
        setContextView(column, view);

        hudson.model.Job wrapped = column.getJobWrapper(job);
        assertNotNull("east matches RunMatcher", wrapped.getBuildByNumber(runEast.getNumber()));
        assertNull("west does not match RunMatcher", wrapped.getBuildByNumber(runWest.getNumber()));
        assertEquals("only 1 build visible", 1, wrapped.getBuilds().size());
    }
}
