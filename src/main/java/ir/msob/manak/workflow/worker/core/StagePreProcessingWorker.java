package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
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

    @JobWorker(type = "stage-pre-processing", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String stageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));

        logger.info("Start executing pre-processing job. jobKey={} workflowId={} stageKey={}", job.getKey(), workflowId, stageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflow -> determineInputData(workflow, cycleId, stageKey, vars)
                        .flatMap(inputData -> createStageHistory(stageKey, inputData)
                                .flatMap(stageHistory -> {
                                    WorkflowUtil.findCycle(workflow, cycleId)
                                            .getStagesHistory()
                                            .add(stageHistory);

                                    return workflowService.update(workflow, userService.getSystemUser())
                                            .thenReturn(stageHistory);
                                })))
                .doOnSuccess(stage -> logger.info("Pre-processing stage saved successfully. stageId={}", stage.getId()))
                .flatMap(stageHistory -> recordWorkerHistory(workflowId).then(prepareResult(stageHistory)))
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Pre-processing job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Pre-processing job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<Workflow.StageHistory> createStageHistory(String stageKey, Map<String, Object> inputData) {
        return Mono.just(Workflow.StageHistory.builder()
                .id(idService.newId()).stageKey(stageKey)
                .executionStatus(Workflow.StageExecutionStatus.INITIALIZED)
                .stageInput(inputData).startedAt(Instant.now()).build());
    }

    private Mono<Map<String, Object>> determineInputData(Workflow workflowDto, String cycleId, String stageKey, Map<String, Object> processVariable) {
        return Mono.fromSupplier(() -> {
            Map<String, Object> workflowContext = workflowDto.getContext();
            Workflow.Cycle cycle = WorkflowUtil.findCycle(workflowDto, cycleId);
            Map<String, Object> cycleContext = cycle.getContext();
            WorkflowSpecification.StageSpec stageSpec = WorkflowUtil.findStageSpecByKey(workflowDto, stageKey);
            Map<String, Object> inputData = new HashMap<>();
            if (stageSpec.getInputMapping() != null) {
                stageSpec.getInputMapping().forEach((inputKey, mappingValue) -> {
                    Object value = resolveMapping(mappingValue, workflowContext, cycleContext, processVariable);
                    if (value != null) {
                        inputData.put(inputKey, value);
                    }
                });
            }
            return inputData;
        });
    }

    private Object resolveMapping(Object mappingValue,
                                  Map<String, Object> workflowContext,
                                  Map<String, Object> cycleContext,
                                  Map<String, Object> processVariable) {
        if (!(mappingValue instanceof String mappingStr)) {
            return mappingValue;
        }

        if (!mappingStr.startsWith(VARIABLE_START_CHAR)) {
            return mappingStr;
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

    private Mono<Void> recordWorkerHistory(String workflowId) {
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.SUCCESS, null);
    }

    private Mono<Map<String, Object>> prepareResult(Workflow.StageHistory stageHistory) {
        return Mono.just(Map.of(
                STAGE_HISTORY_ID_KEY, stageHistory.getId(),
                STAGE_EXECUTION_STATUS_KEY, Workflow.StageExecutionStatus.INITIALIZED,
                STAGE_EXECUTION_ERROR_KEY, ""
        ));
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Pre-processing job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}