package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.worker.WorkerUtils;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.ConditionEvaluator;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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
    private final ConditionEvaluator conditionEvaluator;

    @JobWorker(type = "stage-decision", autoComplete = false)
    public void execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableUtils.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableUtils.safeString(vars.get(CYCLE_ID_KEY));
        String previousStageHistoryId = VariableUtils.safeString(vars.get(STAGE_HISTORY_ID_KEY));
        String previousStageKey = VariableUtils.safeString(vars.get(STAGE_KEY_KEY));

        logger.info("Starting 'stage-decision' job. jobKey={} workflowId={} previousStageKey={}", job.getKey(), workflowId, previousStageKey);

         workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflowDto -> determineNextStage(workflowDto, cycleId, previousStageHistoryId, previousStageKey, vars))
                .flatMap(nextStage -> workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.SUCCESS, null)
                        .then(Mono.just(nextStage)))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Stage-decision job completed successfully. jobKey={} previousStageKey={}", job.getKey(), previousStageKey))
                .doOnError(ex -> logger.error("Stage-decision job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex))
                .subscribe();
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowSpecification.StageSpec stageSpec) {
        return Mono.just(Map.of(
                STAGE_TYPE_KEY, stageSpec.getStage().getType(),
                STAGE_KEY_KEY, stageSpec.getStage().getKey()
        ));
    }

    private Mono<WorkflowSpecification.StageSpec> determineNextStage(WorkflowDto workflowDto, String cycleId, String previousStageHistoryId, String previousStageKey, Map<String, Object> processVars) {
        if (previousStageKey == null || previousStageKey.isBlank()) {
            return Mono.just(WorkflowUtil.findFirstStageSpec(workflowDto));
        } else {
            return Mono.just(findNextStage(workflowDto, cycleId, previousStageHistoryId, previousStageKey, processVars));
        }
    }

    private WorkflowSpecification.StageSpec findNextStage(WorkflowDto workflowDto, String cycleId, String previousStageHistoryId, String previousStageKey, Map<String, Object> processVars) {
        WorkflowSpecification.StageSpec currentStage = WorkflowUtil.findStageSpecByKey(workflowDto, previousStageKey);
        Workflow.StageHistory stageHistory = WorkflowUtil.findStageHistory(workflowDto, cycleId, previousStageHistoryId);
        return getNextStage(workflowDto, currentStage.getTransitions(), stageHistory, cycleId, processVars);
    }

    private WorkflowSpecification.StageSpec getNextStage(WorkflowDto workflowDto, List<WorkflowSpecification.Transition> transitions, Workflow.StageHistory stageHistory, String cycleId, Map<String, Object> processVars) {
        Workflow.Cycle cycle = WorkflowUtil.findCycle(workflowDto, cycleId);
        Map<String, Object> workflowContext = workflowDto.getContext();
        Map<String, Object> cycleContext = cycle != null ? cycle.getContext() : null;
        Map<String, Object> stageOutput = stageHistory != null ? stageHistory.getStageOutput() : Map.of();

        List<String> failedReasons = new ArrayList<>();

        for (WorkflowSpecification.Transition transition : transitions) {
            Map<String, Object> conds = transition.getOn();
            try {
                boolean match = conditionEvaluator.evaluateConditions(conds, workflowContext, cycleContext, processVars, stageOutput);
                if (match) {
                    return WorkflowUtil.findStageSpecByKey(workflowDto, transition.getGoTo());
                } else {
                    failedReasons.add("transition to '" + transition.getGoTo() + "' didn't match");
                }
            } catch (Exception ex) {
                logger.warn("Error evaluating transition to '{}': {}", transition.getGoTo(), ex.getMessage(), ex);
                failedReasons.add("transition to '" + transition.getGoTo() + "' error: " + ex.getMessage());
            }
        }

        String msg = "No valid transition found for the current stage and variables. Reasons: " + String.join("; ", failedReasons);
        throw new DataNotFoundException(msg);
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Stage-decision job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, WorkerUtils.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
