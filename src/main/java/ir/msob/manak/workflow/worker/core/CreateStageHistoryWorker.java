package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class CreateStageHistoryWorker {

    private static final Logger logger = LoggerFactory.getLogger(CreateStageHistoryWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;
    private final IdService idService;

    @JobWorker(type = "create-stage-history", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String stageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));

        logger.info("Start executing create-stage-history job. jobKey={} workflowId={} stageKey={}", job.getKey(), workflowId, stageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflow -> prepareStageHistory(stageKey)
                        .flatMap(stageHistory -> {
                            WorkflowUtil.findCycle(workflow, cycleId).getStagesHistory().add(stageHistory);
                            return workflowService.update(workflow, userService.getSystemUser())
                                    .thenReturn(stageHistory);
                        })

                )
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Create-stage-history job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Create-stage-history job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<Workflow.StageHistory> prepareStageHistory(String stageKey) {
        return Mono.just(Workflow.StageHistory.builder()
                .id(idService.newId())
                .stageKey(stageKey)
                .executionStatus(Workflow.StageExecutionStatus.INITIALIZED)
                .startedAt(Instant.now())
                .build());
    }

    private Mono<Map<String, Object>> prepareResult(Workflow.StageHistory stageHistory) {
        return Mono.just(Map.of(STAGE_HISTORY_ID_KEY, stageHistory.getId()));
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Create-stage-history job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
