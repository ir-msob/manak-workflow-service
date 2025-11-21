package ir.msob.manak.workflow.worker.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class StagePreProcessingWorker {
    private static final Logger logger = LoggerFactory.getLogger(StagePreProcessingWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;
    private final IdService idService;
    private ObjectMapper objectMapper;

    /**
     * Executes the pre-processing stage of a workflow.
     * Steps:
     * 1. Load workflow by ID
     * 2. Determine input data for the stage
     * 3. Create stage history with input data
     * 4. Update workflow in DB
     * 5. Record worker history and prepare process variables for Camunda
     */
    @JobWorker(type = "stage-pre-processing", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String stageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));

        logger.info("Starting stage pre-processing job. jobKey={}, workflowId={}, stageKey={}", job.getKey(), workflowId, stageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflow -> determineInputData(workflow, cycleId, stageKey, vars)
                        .flatMap(inputData -> createStageHistory(workflow, stageKey, inputData)
                                .flatMap(stageHistory -> {
                                    // Add stage history to the corresponding cycle
                                    WorkflowUtil.findCycle(workflow, cycleId)
                                            .getStagesHistory()
                                            .add(stageHistory);

                                    return workflowService.update(workflow, userService.getSystemUser())
                                            .thenReturn(stageHistory);
                                })))
                .doOnSuccess(stage -> logger.info("Pre-processing stage saved successfully. stageId={}", stage.getId()))
                .flatMap(stageHistory -> recordWorkerHistory(workflowId)
                        .then(prepareResult(stageHistory)))
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Pre-processing job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Pre-processing job failed. jobKey={}, error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    /**
     * Creates a new StageHistory object for the stage.
     */
    private Mono<Workflow.StageHistory> createStageHistory(WorkflowDto workflow, String stageKey, Map<String, Object> inputData) {
        WorkflowSpecification.StageSpec stageSpec = WorkflowUtil.findStageSpecByKey(workflow, stageKey);
        return Mono.just(Workflow.StageHistory.builder()
                .id(idService.newId())
                .stage(stageSpec.getStage())
                .executionStatus(Workflow.StageExecutionStatus.INITIALIZED)
                .stageInput(inputData)
                .startedAt(Instant.now())
                .build());
    }

    /**
     * Determines input data for a stage according to stage input mappings.
     */
    private Mono<Map<String, Object>> determineInputData(Workflow workflow, String cycleId, String stageKey, Map<String, Object> processVariable) {
        return Mono.fromSupplier(() -> {
            Map<String, Object> workflowContext = workflow.getContext();
            Workflow.Cycle cycle = WorkflowUtil.findCycle(workflow, cycleId);
            Map<String, Object> cycleContext = cycle.getContext();
            Map<String, Object> specificationContext = workflow.getSpecification().getContext();
            WorkflowSpecification.StageSpec stageSpec = WorkflowUtil.findStageSpecByKey(workflow, stageKey);

            Map<String, Object> inputData = clone(specificationContext);
            if (stageSpec.getInputMapping() != null) {
                stageSpec.getInputMapping().forEach((inputKey, mappingValue) -> {
                    Object value = resolveMapping(mappingValue, workflowContext, cycleContext, processVariable);
                    if (value != null) {
                        inputData.put(inputKey, value);
                    }
                });
            }
            logger.debug("Determined input data for stage '{}': {}", stageKey, inputData.keySet());
            return inputData;
        });
    }

    @SneakyThrows
    private Map<String, Object> clone(Map<String, Object> specificationContext) {
        return objectMapper.readValue(objectMapper.writeValueAsString(specificationContext), new TypeReference<Map<String, Object>>() {
            @Override
            public Type getType() {
                return super.getType();
            }
        });
    }

    /**
     * Resolves a mapping value based on workflow, cycle contexts or process variables.
     */
    private Object resolveMapping(Object mappingValue,
                                  Map<String, Object> workflowContext,
                                  Map<String, Object> cycleContext,
                                  Map<String, Object> processVariable) {
        if (!(mappingValue instanceof String mappingStr)) {
            return mappingValue; // literal value
        }

        if (!mappingStr.startsWith(VARIABLE_START_CHAR)) {
            return mappingStr; // literal string
        }

        String expr = mappingStr.substring(1);
        if (expr.startsWith(WORKFLOW_CONTEXT_KEY + ".")) {
            return getValueByPath(workflowContext, expr.substring((WORKFLOW_CONTEXT_KEY + ".").length()));
        } else if (expr.startsWith(CYCLE_CONTEXT_KEY + ".")) {
            return getValueByPath(cycleContext, expr.substring((CYCLE_CONTEXT_KEY + ".").length()));
        } else if (expr.startsWith(PROCESS_VARIABLE_KEY + ".")) {
            return getValueByPath(processVariable, expr.substring((PROCESS_VARIABLE_KEY + ".").length()));
        }
        return null;
    }

    /**
     * Reads a nested value from a Map using a dot-delimited path.
     */
    @SuppressWarnings("unchecked")
    private Object getValueByPath(Map<String, Object> context, String path) {
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
     * Records worker history for the stage.
     */
    private Mono<Void> recordWorkerHistory(String workflowId) {
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.SUCCESS, null);
    }

    /**
     * Prepares the result map to be returned to Camunda.
     */
    private Mono<Map<String, Object>> prepareResult(Workflow.StageHistory stageHistory) {
        return Mono.just(Map.of(
                STAGE_HISTORY_ID_KEY, stageHistory.getId(),
                STAGE_EXECUTION_STATUS_KEY, Workflow.StageExecutionStatus.INITIALIZED,
                STAGE_EXECUTION_ERROR_KEY, ""
        ));
    }

    /**
     * Handles errors, records them in workflow history, sends error to Camunda, then rethrows.
     */
    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Pre-processing job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
