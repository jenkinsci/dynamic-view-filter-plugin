package io.jenkins.plugins.dynamic_view_filter;

import hudson.model.BooleanParameterValue;
import hudson.model.FileParameterValue;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import java.util.regex.Pattern;

/**
 * Shared utilities for parameter value extraction and regex compilation.
 */
final class ParameterUtils {

    private ParameterUtils() {}

    static Pattern toPattern(String regex) {
        if (regex == null || regex.isEmpty()) {
            return null;
        }
        return Pattern.compile(regex);
    }

    static String getStringValue(ParameterValue value) {
        if (value instanceof StringParameterValue) {
            return (String) ((StringParameterValue) value).getValue();
        } else if (value instanceof BooleanParameterValue) {
            return String.valueOf(((BooleanParameterValue) value).value);
        } else if (value instanceof FileParameterValue) {
            return ((FileParameterValue) value).getOriginalFileName();
        }
        return String.valueOf(value);
    }

    static String getParamValue(Run<?, ?> run, String paramName) {
        ParametersAction action = run.getAction(ParametersAction.class);
        if (action == null) {
            return null;
        }
        for (ParameterValue pv : action.getParameters()) {
            if (paramName != null && paramName.equals(pv.getName())) {
                return String.valueOf(pv.getValue());
            }
        }
        return null;
    }

    static boolean matchesPattern(Pattern p, String m) {
        if (p == null) {
            return true;
        }
        if (m == null) {
            return false;
        }
        return p.matcher(m).matches();
    }
}
