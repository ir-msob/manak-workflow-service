package ir.msob.manak.workflow.worker.util;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static ir.msob.manak.workflow.worker.Constants.WORKER_EXECUTION_ERROR_KEY;
import static ir.msob.manak.workflow.worker.Constants.WORKER_EXECUTION_STATUS_KEY;

public class VariableHelper {
    public static String safeString(Object o) {
        if (o == null) return null;
        return Objects.toString(o, null);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> safeMapStringString(Object o) {
        if (o == null) return Collections.emptyMap();
        try {
            if (o instanceof Map<?, ?> map) {
                return (Map<String, String>) map;
            }
            throw new CommonRuntimeException("Cannot convert Map");
        } catch (ClassCastException ex) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> safeMapStringObject(Object o) {
        if (o == null) return Collections.emptyMap();
        try {
            if (o instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            throw new CommonRuntimeException("Cannot convert Map");
        } catch (ClassCastException ex) {
            return Collections.emptyMap();
        }
    }

    public static int safeInt(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public static Map<String, Object> prepareErrorResult(String errorMessage) {
        return Map.of(
                WORKER_EXECUTION_STATUS_KEY, Workflow.WorkerExecutionStatus.ERROR,
                WORKER_EXECUTION_ERROR_KEY, errorMessage
        );
    }
}
