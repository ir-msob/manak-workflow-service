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

import java.util.HashMap;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class StageInputDataWorker {

    private static final Logger logger = LoggerFactory.getLogger(StageInputDataWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    @JobWorker(type = "stage-input-data", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String stageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));
        String stageHistoryId = VariableHelper.safeString(vars.get(STAGE_HISTORY_ID_KEY));

        logger.info("Start executing create-stage-history job. jobKey={} workflowId={} stageKey={}", job.getKey(), workflowId, stageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflow -> determineInputData(workflow, cycleId, stageKey, vars)
                        .flatMap(inputData -> update(workflow, stageHistoryId, cycleId, inputData)
                                .flatMap(updatedWorkflow -> workflowService.update(updatedWorkflow, userService.getSystemUser())
                                        .thenReturn(updatedWorkflow))
                        )
                )
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Create-stage-history job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Create-stage-history job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<WorkflowDto> update(WorkflowDto workflow, String stageHistoryId, String cycleId, Map<String, Object> inputData) {
        Workflow.StageHistory stageHistory = WorkflowUtil.findStageHistory(workflow, cycleId, stageHistoryId);
        stageHistory.setStageInput(inputData);
        return Mono.just(workflow);
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowDto workflow) {
        return Mono.just(Map.of());
    }

    private Mono<Map<String, Object>> determineInputData(Workflow workflowDto, String cycleId, String stageKey, Map<String, Object> processVariable) {
        return Mono.fromSupplier(() -> {
            Map<String, Object> workflowContext = workflowDto.getContext();
            Workflow.Cycle cycle = WorkflowUtil.findCycle(workflowDto, cycleId);
            Map<String, Object> cycleContext = cycle.getContext();
            WorkflowSpecification.StageSpec stageSpec = WorkflowUtil.findStageSpecByKey(workflowDto, stageKey);

            Map<String, Object> inputData = new HashMap<>();
            if (stageSpec.getInputMapping() != null) {
                stageSpec.getInputMapping().forEach((inputKey, mappingPath) -> {
                    Object value = resolveMapping(mappingPath, workflowContext, cycleContext, processVariable);
                    if (value != null) {
                        inputData.put(inputKey, value);
                    }
                });
            }
            return inputData;
        });
    }

    private Object resolveMapping(String mappingPath, Map<String, Object> workflowContext,
                                  Map<String, Object> cycleContext, Map<String, Object> processVariable) {
        if (mappingPath.startsWith("workflowContext.")) {
            return getValueByPath(workflowContext, mappingPath.substring("workflowContext.".length()));
        } else if (mappingPath.startsWith("cycleContext.")) {
            return getValueByPath(cycleContext, mappingPath.substring("cycleContext.".length()));
        } else if (mappingPath.startsWith("processVariable.")) {
            return getValueByPath(processVariable, mappingPath.substring("processVariable.".length()));
        } else {
            return mappingPath;
        }
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

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Create-stage-history job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
