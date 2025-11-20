package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class PostProcessingStageWorker {

    private static final Logger logger = LoggerFactory.getLogger(PostProcessingStageWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    @JobWorker(type = "post-processing-stage", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String stageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));
        String stageHistoryId = VariableHelper.safeString(vars.get(STAGE_HISTORY_ID_KEY));
        Map<String, Object> stageOutput = VariableHelper.safeMapStringObject(vars.get(STAGE_OUTPUT_KEY));

        logger.info("Start executing post-processing job. jobKey={} workflowId={} stageKey={}", job.getKey(), workflowId, stageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflow -> updateStageHistory(workflow, stageHistoryId, cycleId, stageOutput))
                // pass stageKey and vars so updateContext can resolve sources like $processVariable.*
                .flatMap(workflow -> updateContext(workflow, stageKey, cycleId, stageOutput, vars))
                // update workflow in DB and then return collected process variables to be set on the process
                .flatMap(tuple -> {
                    WorkflowDto wf = tuple.getT1();
                    Map<String, Object> processVarsToSet = tuple.getT2();
                    return workflowService.update(wf, userService.getSystemUser())
                            .thenReturn(processVarsToSet);
                })
                .flatMap(this::prepareResult) // prepareResult returns the map of process variables for Camunda
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Post-processing job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Post-processing job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<WorkflowDto> updateStageHistory(WorkflowDto workflow, String stageHistoryId, String cycleId, Map<String, Object> outputData) {
        Workflow.StageHistory stageHistory = WorkflowUtil.findStageHistory(workflow, cycleId, stageHistoryId);
        stageHistory.setStageOutput(outputData);
        stageHistory.setExecutionStatus(Workflow.StageHistoryExecutionStatus.SUCCESS);
        stageHistory.setEndedAt(Instant.now());
        return Mono.just(workflow);
    }

    /**
     * Update contexts according to output mapping and collect variables that should be returned to the process.
     * <p>
     * Returns Tuple2:
     * - T1 = updated WorkflowDto
     * - T2 = Map of process variables to return (keys are variable names without "processVariable." prefix)
     */
    private Mono<Tuple2<WorkflowDto, Map<String, Object>>> updateContext(WorkflowDto workflow,
                                                                         String stageKey,
                                                                         String cycleId,
                                                                         Map<String, Object> stageOutput,
                                                                         Map<String, Object> processVariable) {
        Workflow.Cycle cycle = WorkflowUtil.findCycle(workflow, cycleId);
        WorkflowSpecification.StageSpec stageSpec = WorkflowUtil.findStageSpecByKey(workflow, stageKey);

        // Expect outputMapping to be Map<String, Object> to allow literal objects/arrays as mapping values.
        Map<String, Object> outputMapping = stageSpec.getOutputMapping();
        if (outputMapping == null || outputMapping.isEmpty()) {
            return Mono.just(Tuples.of(workflow, Map.of()));
        }

        Map<String, Object> processVarsToReturn = new HashMap<>();

        outputMapping.forEach((destObj, srcObj) -> {
            String destExpr = Objects.toString(destObj, null);
            if (destExpr == null) return;

            // Resolve source value (can read from workflowContext, cycleContext, processVariable, or stageOutput,
            // or be a literal value).
            Object value = resolveSourceValue(srcObj, workflow.getContext(), cycle.getContext(), processVariable, stageOutput);

            if (value == null) {
                // skip nulls
                return;
            }

            // Destination: strip leading $ if present
            String dest = destExpr.startsWith(VARIABLE_START_CHAR) ? destExpr.substring(1) : destExpr;

            if (dest.startsWith(CYCLE_CONTEXT_KEY + ".")) {
                String path = dest.substring((CYCLE_CONTEXT_KEY + ".").length());
                setValueByPath(cycle.getContext(), path, value);

            } else if (dest.startsWith(WORKFLOW_CONTEXT_KEY + ".")) {
                String path = dest.substring((WORKFLOW_CONTEXT_KEY + ".").length());
                setValueByPath(workflow.getContext(), path, value);

            } else if (dest.startsWith(PROCESS_VARIABLE_KEY + ".")) {
                String varName = dest.substring((PROCESS_VARIABLE_KEY + ".").length());
                processVarsToReturn.put(varName, value);

            } else {
                // Unknown destination scope -> log a warning
                logger.warn("Unknown output mapping destination '{}', ignoring.", destExpr);
            }
        });

        return Mono.just(Tuples.of(workflow, processVarsToReturn));
    }

    /**
     * Resolve a source object according to rules:
     * - if srcObj is not a String -> treat as literal (Map/List/Number/etc.)
     * - if srcObj is a String and does NOT start with $ -> literal string
     * - if srcObj is a String starting with $:
     * - $workflowContext.<path> -> read from workflow context
     * - $cycleContext.<path> -> read from cycle context
     * - $processVariable.<path> -> read from incoming process variables
     * - $<path-without-prefix> -> read from stageOutput
     */
    private Object resolveSourceValue(Object srcObj,
                                      Map<String, Object> workflowContext,
                                      Map<String, Object> cycleContext,
                                      Map<String, Object> processVariable,
                                      Map<String, Object> stageOutput) {

        if (!(srcObj instanceof String srcStr)) {
            // literal object (Map, List, Number, etc.)
            return srcObj;
        }

        if (!srcStr.startsWith(VARIABLE_START_CHAR)) {
            // literal string
            return srcObj;
        }

        String expr = srcStr.substring(1);

        if (expr.startsWith(WORKFLOW_CONTEXT_KEY + ".")) {
            String path = expr.substring((WORKFLOW_CONTEXT_KEY + ".").length());
            return getValueByPath(workflowContext, path);

        } else if (expr.startsWith(CYCLE_CONTEXT_KEY + ".")) {
            String path = expr.substring((CYCLE_CONTEXT_KEY + ".").length());
            return getValueByPath(cycleContext, path);

        } else if (expr.startsWith(PROCESS_VARIABLE_KEY + ".")) {
            String path = expr.substring((PROCESS_VARIABLE_KEY + ".").length());
            return getValueByPath(processVariable, path);

        } else {
            // No prefix -> treat as lookup into stageOutput
            return getValueByPath(stageOutput, expr);
        }
    }

    /**
     * Read nested value from a Map using dot-delimited path.
     * Returns null if any path segment is missing or non-map encountered.
     */
    @SuppressWarnings("unchecked")
    private Object getValueByPath(Map<String, Object> context, String path) {
        if (context == null) return null;
        String[] keys = path.split("\\.");
        Object current = context;
        for (String key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(key);
            if (current == null) return null;
        }
        return current;
    }

    /**
     * Put value into a nested map by dot-delimited path. Creates intermediate maps when needed.
     */
    @SuppressWarnings("unchecked")
    private void setValueByPath(Map<String, Object> context, String path, Object value) {
        if (context == null) return;
        String[] keys = path.split("\\.");
        Map<String, Object> current = context;
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            if (i == keys.length - 1) {
                current.put(k, value);
                return;
            }
            Object next = current.get(k);
            if (!(next instanceof Map)) {
                Map<String, Object> newMap = new HashMap<>();
                current.put(k, newMap);
                current = newMap;
            } else {
                current = (Map<String, Object>) next;
            }
        }
    }

    /**
     * Prepare result to send to Camunda: return the map of process variables that were collected.
     * If there are no variables to return, an empty map is returned.
     */
    private Mono<Map<String, Object>> prepareResult(Map<String, Object> processVars) {
        return Mono.just(processVars != null ? processVars : Map.of());
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Post-processing job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
