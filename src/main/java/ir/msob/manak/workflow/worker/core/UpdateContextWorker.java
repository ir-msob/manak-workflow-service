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

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class UpdateContextWorker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateContextWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    @JobWorker(type = "update-context", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String stageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));
        String stageHistoryId = VariableHelper.safeString(vars.get(STAGE_HISTORY_ID_KEY));
        Map<String, Object> stageOutput = VariableHelper.safeMapStringObject(vars.get(STAGE_OUTPUT_KEY));

        logger.info("Start executing create-stage-history job. jobKey={} workflowId={} stageKey={}", job.getKey(), workflowId, stageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflow -> updateStageOutput(workflow, stageHistoryId, cycleId, stageOutput))
                .flatMap(workflow -> updateContext(workflow, stageHistoryId, cycleId, stageOutput))
                .flatMap(workflow -> workflowService.update(workflow, userService.getSystemUser()))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Create-stage-history job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Create-stage-history job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<WorkflowDto> updateStageOutput(WorkflowDto workflow, String stageHistoryId, String cycleId, Map<String, Object> outputData) {
        WorkflowUtil.findCycle(workflow, cycleId)
                .getStagesHistory()
                .stream()
                .filter(stageHistory -> stageHistory.getId().equalsIgnoreCase(stageHistoryId))
                .findFirst()
                .ifPresent(stageHistory -> stageHistory.setStageOutput(outputData));
        return Mono.just(workflow);
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowDto workflow) {
        return Mono.just(Map.of());
    }

    private Mono<WorkflowDto> updateContext(
            WorkflowDto workflow,
            String cycleId,
            String stageKey,
            Map<String, Object> stageOutput) {

        Workflow.Cycle cycle = WorkflowUtil.findCycle(workflow, cycleId);

        WorkflowSpecification.StageSpec stageSpec = WorkflowUtil.findStageSpecByKey(workflow, stageKey);

        Map<String, String> outputMapping = stageSpec.getOutputMapping();
        if (outputMapping == null || outputMapping.isEmpty()) {
            return Mono.just(workflow);
        }

        outputMapping.forEach((outputFieldName, mappingPath) -> {
            Object value = stageOutput.get(outputFieldName);
            if (value == null) {
                return; // skip null fields
            }

            if (mappingPath.startsWith("cycleContext.")) {
                String field = mappingPath.substring("cycleContext.".length());
                cycle.getContext().put(field, value);

            } else if (mappingPath.startsWith("workflowContext.")) {
                String field = mappingPath.substring("workflowContext.".length());
                workflow.getContext().put(field, value);

            } else {
                logger.warn("Unknown mapping scope '{}', ignoring.", mappingPath);
            }
        });

        return Mono.just(workflow);
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Create-stage-history job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
