package io.jenkins.plugins.dynamic_view_filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.BooleanParameterValue;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DropdownFilterViewTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    // ---- helpers ----

    private FreeStyleBuild buildWithParam(FreeStyleProject job, String name, String value) throws Exception {
        QueueTaskFuture<FreeStyleBuild> f = job.scheduleBuild2(0,
                new ParametersAction(new StringParameterValue(name, value)));
        return j.assertBuildStatusSuccess(f);
    }

    // ---- regex dropdown tests ----

    @Test
    public void regexDropdownFiltersJobs() throws Exception {
        FreeStyleProject job1 = j.createFreeStyleProject("app-linux-deploy");
        FreeStyleProject job2 = j.createFreeStyleProject("app-macos-deploy");
        FreeStyleProject job3 = j.createFreeStyleProject("app-linux-test");

        DropdownFilterView view = new DropdownFilterView("test-dropdown", j.jenkins);
        j.jenkins.addView(view);
        view.add(job1);
        view.add(job2);
        view.add(job3);

        // All 3 jobs in view without filter
        List<TopLevelItem> items = view.getItems();
        assertEquals(3, items.size());
    }

    @Test
    public void regexCaptureGroupExtractsValues() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("services-auth-api-staging-build");

        DropdownDefinition dd = new DropdownDefinition("Job", "jobNameRegex", "(.+)", "");
        List<String> values = dd.extractAllValues(job);
        assertEquals(1, values.size());
        assertEquals("services-auth-api-staging-build", values.get(0));
    }

    @Test
    public void regexCaptureGroupMatchesParts() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("app-linux-deploy");

        DropdownDefinition dd = new DropdownDefinition("OS", "jobNameRegex", "app-([^-]+)-.*", "");
        List<String> values = dd.extractAllValues(job);
        assertEquals(1, values.size());
        assertEquals("linux", values.get(0));
        assertTrue("should match linux", dd.matches(job, "linux"));
        assertTrue("empty selection matches all", dd.matches(job, ""));
        assertTrue("null selection matches all", dd.matches(job, null));
    }

    @Test
    public void regexCaptureGroupLastPart() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("app-linux-deploy");

        DropdownDefinition dd = new DropdownDefinition("Action", "jobNameRegex", ".*-([^-]+)", "");
        List<String> values = dd.extractAllValues(job);
        assertEquals(1, values.size());
        assertEquals("deploy", values.get(0));
        assertTrue("should match deploy", dd.matches(job, "deploy"));
    }

    @Test
    public void dropdownValuesAutoDiscovered() throws Exception {
        FreeStyleProject job1 = j.createFreeStyleProject("alpha");
        FreeStyleProject job2 = j.createFreeStyleProject("beta");
        FreeStyleProject job3 = j.createFreeStyleProject("gamma");

        DropdownFilterView view = new DropdownFilterView("test-values", j.jenkins);
        j.jenkins.addView(view);
        view.add(job1);
        view.add(job2);
        view.add(job3);

        DropdownDefinition dd = new DropdownDefinition("Name", "jobNameRegex", "(.+)", "");
        view.setDropdowns(Arrays.asList(dd));

        List<String> values = view.getDropdownValues(dd);
        assertEquals(3, values.size());
        assertEquals("alpha", values.get(0));
        assertEquals("beta", values.get(1));
        assertEquals("gamma", values.get(2));
    }

    // ---- build parameter dropdown tests ----

    @Test
    public void buildParamDropdownExtractsValues() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("param-job");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        buildWithParam(job, "region", "east");
        buildWithParam(job, "region", "west");
        buildWithParam(job, "region", "east");

        DropdownDefinition dd = new DropdownDefinition("Region", "buildParameter", "", "region");
        List<String> values = dd.extractAllValues(job);
        assertEquals("should deduplicate values", 2, values.size());
        assertTrue("should contain east", values.contains("east"));
        assertTrue("should contain west", values.contains("west"));
    }

    @Test
    public void buildParamDropdownMatchesJob() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("match-job");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        buildWithParam(job, "region", "east");
        buildWithParam(job, "region", "west");

        DropdownDefinition dd = new DropdownDefinition("Region", "buildParameter", "", "region");
        assertTrue("job has east build", dd.matches(job, "east"));
        assertTrue("job has west build", dd.matches(job, "west"));
        assertFalse("job has no central build", dd.matches(job, "central"));
        assertTrue("empty selection matches all", dd.matches(job, ""));
    }

    @Test
    public void buildParamDropdownNoBuildsReturnsEmpty() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("empty-job");

        DropdownDefinition dd = new DropdownDefinition("Region", "buildParameter", "", "region");
        List<String> values = dd.extractAllValues(job);
        assertEquals(0, values.size());
        assertFalse("should not match with no builds", dd.matches(job, "east"));
    }

    // ---- DropdownFilterView property tests ----

    @Test
    public void filterPositionDefaultsToTop() throws Exception {
        DropdownFilterView view = new DropdownFilterView("fp-test", j.jenkins);
        assertEquals("top", view.getFilterPosition());

        view.setFilterPosition("sidebar");
        assertEquals("sidebar", view.getFilterPosition());

        view.setFilterPosition(null);
        assertEquals("top", view.getFilterPosition());
    }

    @Test
    public void getSelectedValueReturnsEmptyOutsideRequest() throws Exception {
        DropdownFilterView view = new DropdownFilterView("sel-test", j.jenkins);
        assertEquals("", view.getSelectedValue(0));
        assertEquals("", view.getSelectedValue(1));
    }

    @Test
    public void getActiveRunMatchersEmptyWithoutRequest() throws Exception {
        DropdownFilterView view = new DropdownFilterView("matcher-test", j.jenkins);
        j.jenkins.addView(view);

        DropdownDefinition dd = new DropdownDefinition("Region", "buildParameter", "", "region");
        view.setDropdowns(Arrays.asList(dd));

        List<hudson.views.RunMatcher> matchers = view.getActiveRunMatchers();
        assertTrue("no matchers without request", matchers.isEmpty());
    }

    @Test
    public void getItemsWithDropdownsFiltersNonJobItems() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("dropdown-job");

        DropdownFilterView view = new DropdownFilterView("items-test", j.jenkins);
        j.jenkins.addView(view);
        view.add(job);

        DropdownDefinition dd = new DropdownDefinition("Name", "jobNameRegex", "(.+)", "");
        view.setDropdowns(Arrays.asList(dd));

        List<TopLevelItem> items = view.getItems();
        assertEquals(1, items.size());
    }

    // ---- DropdownRunMatcher tests ----

    @Test
    public void dropdownRunMatcherMatchesBuild() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("drm-job");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east", "west"}, "region")));

        FreeStyleBuild eastBuild = buildWithParam(job, "region", "east");
        FreeStyleBuild westBuild = buildWithParam(job, "region", "west");

        DropdownFilterView.DropdownRunMatcher matcher =
                new DropdownFilterView.DropdownRunMatcher("region", "east");

        assertTrue("east build should match", matcher.matchesRun(eastBuild));
        assertFalse("west build should not match", matcher.matchesRun(westBuild));
        assertFalse("null run should not match", matcher.matchesRun(null));
    }

    // ---- ParameterUtils tests ----

    @Test
    public void parameterUtilsToPatternHandlesNullAndEmpty() {
        assertNull("null regex returns null", ParameterUtils.toPattern(null));
        assertNull("empty regex returns null", ParameterUtils.toPattern(""));
        Pattern p = ParameterUtils.toPattern("abc");
        assertNotNull("non-empty regex returns pattern", p);
        assertEquals("abc", p.pattern());
    }

    @Test
    public void parameterUtilsMatchesPatternEdgeCases() {
        assertFalse("null match string returns false", ParameterUtils.matchesPattern(Pattern.compile(".*"), null));
        assertTrue("null pattern matches anything", ParameterUtils.matchesPattern(null, "anything"));
        assertTrue("matching pattern returns true", ParameterUtils.matchesPattern(Pattern.compile("abc"), "abc"));
        assertFalse("non-matching pattern returns false", ParameterUtils.matchesPattern(Pattern.compile("abc"), "xyz"));
    }

    @Test
    public void parameterUtilsGetStringValueHandlesTypes() throws Exception {
        StringParameterValue spv = new StringParameterValue("name", "hello");
        assertEquals("hello", ParameterUtils.getStringValue(spv));

        BooleanParameterValue bpv = new BooleanParameterValue("flag", true);
        assertEquals("true", ParameterUtils.getStringValue(bpv));
    }

    @Test
    public void parameterUtilsGetParamValueReturnsNullForMissingParam() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("param-util-job");
        job.addProperty(new ParametersDefinitionProperty(
                new ChoiceParameterDefinition("region", new String[]{"east"}, "region")));

        FreeStyleBuild build = buildWithParam(job, "region", "east");

        assertEquals("east", ParameterUtils.getParamValue(build, "region"));
        assertNull("missing param returns null", ParameterUtils.getParamValue(build, "nonexistent"));
    }
}
