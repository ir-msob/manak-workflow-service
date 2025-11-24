package ir.msob.manak.workflow.worker.util;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
public class ConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluator.class);

    /**
     * Evaluate all conditions in the "on" map. All must be true (logical AND).
     */
    public boolean evaluateConditions(Map<String, Object> conditions,
                                      Map<String, Object> workflowContext,
                                      Map<String, Object> cycleContext,
                                      Map<String, Object> processVars,
                                      Map<String, Object> stageOutput) {

        if (conditions == null || conditions.isEmpty()) return false;

        for (Map.Entry<String, Object> e : conditions.entrySet()) {
            String rawKey = e.getKey();
            Object expected = e.getValue();

            Object actual = resolveValueFromConditionKey(rawKey, workflowContext, cycleContext, processVars, stageOutput);
            boolean ok = evaluateComparison(actual, expected, workflowContext, cycleContext, processVars, stageOutput);
            if (!ok) {
                logger.debug("Condition failed. key='{}' actual='{}' expected='{}'", rawKey, actual, expected);
                return false;
            }
        }
        return true;
    }

    /**
     * Resolve the left-hand side (condition key) into an actual value.
     * <p>
     * Rules:
     * - keys usually start with VARIABLE_START_CHAR ('$'). Remove it if present.
     * - if key starts with "workflowContext." -> read from workflowContext
     * - if key starts with "cycleContext." -> read from cycleContext
     * - if key starts with "processVariable." -> read from processVars
     * - otherwise -> read from stageOutput
     */
    public Object resolveValueFromConditionKey(String rawKey,
                                               Map<String, Object> workflowContext,
                                               Map<String, Object> cycleContext,
                                               Map<String, Object> processVars,
                                               Map<String, Object> stageOutput) {
        if (rawKey == null) return null;
        String key = rawKey.startsWith(VARIABLE_START_CHAR) ? rawKey.substring(1) : rawKey;

        if (key.startsWith(WORKFLOW_CONTEXT_KEY + ".")) {
            String path = key.substring((WORKFLOW_CONTEXT_KEY + ".").length());
            return getValueByPath(workflowContext, path);
        } else if (key.startsWith(CYCLE_CONTEXT_KEY + ".")) {
            String path = key.substring((CYCLE_CONTEXT_KEY + ".").length());
            return getValueByPath(cycleContext, path);
        } else if (key.startsWith(PROCESS_VARIABLE_KEY + ".")) {
            String path = key.substring((PROCESS_VARIABLE_KEY + ".").length());
            return getValueByPath(processVars, path);
        } else {
            // no prefix -> treat as stageOutput
            return getValueByPath(stageOutput, key);
        }
    }

    /**
     * Evaluate comparison between actual value and expected.
     * <p>
     * expected may be:
     * - literal (String/Number/Boolean) -> equality (strings case-insensitive)
     * - a String starting with $ -> a reference to another context value (will be resolved)
     * - Map with operator(s) -> support $eq, $ne, $in, $not, $exists, $regex, $gt, $gte, $lt, $lte
     * <p>
     * Returns true if condition satisfied.
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateComparison(Object actual,
                                       Object expected,
                                       Map<String, Object> workflowContext,
                                       Map<String, Object> cycleContext,
                                       Map<String, Object> processVars,
                                       Map<String, Object> stageOutput) {
        // null handling
        if (expected == null) {
            return actual == null;
        }

        // If expected is a Map -> operator-based
        if (expected instanceof Map<?, ?> map) {
            Map<String, Object> opMap = (Map<String, Object>) map;
            for (Map.Entry<String, Object> op : opMap.entrySet()) {
                String operator = op.getKey();
                Object operand = op.getValue();
                boolean res = evaluateOperator(actual, operator, operand, workflowContext, cycleContext, processVars, stageOutput);
                if (!res) return false;
            }
            return true;
        }

        // If expected is a String reference like "$workflowContext.x" -> resolve it
        if (expected instanceof String expectedString && expectedString.startsWith(VARIABLE_START_CHAR)) {
            Object resolved = resolveValueFromConditionKey(expectedString, workflowContext, cycleContext, processVars, stageOutput);
            return evaluateComparison(actual, resolved, workflowContext, cycleContext, processVars, stageOutput);
        }

        // primitive comparison
        if (actual == null) return false;

        if (actual instanceof Number actualNumber && expected instanceof Number expectedNumber) {
            return compareNumbers(actualNumber, expectedNumber) == 0;
        }

        // string-ish compare (case-insensitive)
        String aStr = actual.toString();
        String eStr = expected.toString();
        return aStr.equalsIgnoreCase(eStr);
    }

    private boolean evaluateOperator(Object actual,
                                     String operator,
                                     Object operand,
                                     Map<String, Object> workflowContext,
                                     Map<String, Object> cycleContext,
                                     Map<String, Object> processVars,
                                     Map<String, Object> stageOutput) {
        // If operand is a reference string like "$workflowContext.x", resolve it first
        if (operand instanceof String operandString && (operandString).startsWith(VARIABLE_START_CHAR)) {
            operand = resolveValueFromConditionKey(operandString, workflowContext, cycleContext, processVars, stageOutput);
        }

        switch (operator) {
            case "$eq" -> {
                if (actual == null && operand == null) return true;
                if (actual == null || operand == null) return false;
                if (actual instanceof Number actualNumber && operand instanceof Number operandNumber) {
                    return compareNumbers(actualNumber, operandNumber) == 0;
                }
                return actual.toString().equalsIgnoreCase(operand.toString());
            }
            case "$ne", "$not" -> {
                if (operand == null) return actual != null;
                if (actual == null) return true;
                if (actual instanceof Number actualNumber && operand instanceof Number operandNumber) {
                    return compareNumbers(actualNumber, operandNumber) != 0;
                }
                return !actual.toString().equalsIgnoreCase(operand.toString());
            }
            case "$in" -> {
                if (actual == null) return false;
                Collection<?> col = toCollection(operand);
                if (col == null) return false;
                for (Object item : col) {
                    if (item != null && actual.toString().equalsIgnoreCase(item.toString())) return true;
                }
                return false;
            }
            case "$exists" -> {
                boolean want = Boolean.TRUE.equals(operand) || "true".equalsIgnoreCase(String.valueOf(operand));
                return want == (actual != null);
            }
            case "$regex" -> {
                if (actual == null) return false;
                String pattern = String.valueOf(operand);
                return Pattern.compile(pattern).matcher(actual.toString()).find();
            }
            case "$gt", "$gte", "$lt", "$lte" -> {
                Double right = toDouble(operand);
                Double left = toDouble(actual);
                if (left == null || right == null) return false;
                return switch (operator) {
                    case "$gt" -> left > right;
                    case "$gte" -> left >= right;
                    case "$lt" -> left < right;
                    case "$lte" -> left <= right;
                    default -> false;
                };
            }
            case null, default -> {
                logger.debug("Unknown operator in condition: {}", operator);
                return false;
            }
        }
    }

    private Collection<?> toCollection(Object o) {
        if (o == null) return Collections.emptyList();
        if (o instanceof Collection) return (Collection<?>) o;
        if (o.getClass().isArray()) return Arrays.asList((Object[]) o);
        if (o instanceof String s) {
            if (s.contains(",")) return Arrays.asList(s.split(","));
            return List.of(s);
        }
        return List.of(o);
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number number) return (number).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private int compareNumbers(Number a, Number b) {
        double da = a.doubleValue();
        double db = b.doubleValue();
        return Double.compare(da, db);
    }

    /**
     * Read nested value from a Map using dot-delimited path.
     * Returns null if any path segment is missing or non-map encountered.
     */
    @SuppressWarnings("unchecked")
    private Object getValueByPath(Map<String, Object> context, String path) {
        if (context == null) return null;
        if (Strings.isBlank(path)) return null;
        String[] keys = path.split("\\.");
        Object current = context;
        for (String key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(key);
            if (current == null) return null;
        }
        return current;
    }
}
