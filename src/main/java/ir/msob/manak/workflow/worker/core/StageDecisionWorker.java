package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class StageDecisionWorker {

    private static final Logger logger = LoggerFactory.getLogger(StageDecisionWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    @JobWorker(type = "stage-decision", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));
        String previousStageHistoryId = VariableHelper.safeString(vars.get(STAGE_HISTORY_ID_KEY));
        String previousStageKey = VariableHelper.safeString(vars.get(STAGE_KEY_KEY));

        logger.info("Start executing stage-decision job. jobKey={} workflowId={} stageKey={}", job.getKey(), workflowId, previousStageKey);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId))) // Workflow not found exception
                .flatMap(workflowDto -> determineNextStage(workflowDto, cycleId, previousStageHistoryId, previousStageKey))
                .flatMap(nextStage -> workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.SUCCESS, null)
                        .then(Mono.just(nextStage)))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Stage-decision job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Stage-decision job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowSpecification.StageSpec stageSpec) {
        return Mono.just(Map.of(
                STAGE_TYPE_KEY, stageSpec.getType(),
                STAGE_KEY_KEY, stageSpec.getKey()
        ));
    }

    private Mono<WorkflowSpecification.StageSpec> determineNextStage(Workflow workflowDto, String cycleId, String previousStageHistoryId, String previousStageKey) {
        if (Strings.isBlank(previousStageKey)) {
            return Mono.just(WorkflowUtil.findFirstStageSpec(workflowDto));
        } else {
            return Mono.just(findNextStage(workflowDto, cycleId, previousStageHistoryId, previousStageKey));
        }
    }

    private WorkflowSpecification.StageSpec findNextStage(Workflow workflowDto, String cycleId, String previousStageHistoryId, String previousStageKey) {
        WorkflowSpecification.StageSpec currentStage = WorkflowUtil.findStageSpecByKey(workflowDto, previousStageKey);
        Workflow.StageHistory stageHistory = WorkflowUtil.findStageHistory(workflowDto, cycleId, previousStageHistoryId);
        return getNextStage(workflowDto, currentStage.getTransitions(), stageHistory);
    }

    private WorkflowSpecification.StageSpec getNextStage(Workflow workflowDto, List<WorkflowSpecification.Transition> transitions, Workflow.StageHistory stageHistory) {
        for (WorkflowSpecification.Transition transition : transitions) {
            boolean match = transition.getOn().entrySet().stream()
                    .allMatch(e -> {
                        Object value = stageHistory.getStageOutput().get(e.getKey());
                        return value != null && value.toString().equalsIgnoreCase(e.getValue());
                    });
            if (match) {
                return WorkflowUtil.findStageSpecByKey(workflowDto, transition.getGoTo());
            }
        }
        throw new DataNotFoundException("No valid transition found for the current stage and variables");
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Stage-decision job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}

