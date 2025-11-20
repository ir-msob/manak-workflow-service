package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.CYCLE_ID_KEY;
import static ir.msob.manak.workflow.worker.Constants.WORKFLOW_ID_KEY;

@Component
@RequiredArgsConstructor
public class FlowCompleteErrorWorker {

    private static final Logger logger = LoggerFactory.getLogger(FlowCompleteErrorWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    @JobWorker(type = "flow-complete-error", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(vars.get(CYCLE_ID_KEY));

        logger.info("Start executing post-processing job. jobKey={} workflowId={}", job.getKey(), workflowId);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(this::prepareWorkflow)
                .flatMap(workflowDto -> prepareCycle(workflowDto, cycleId))
                .flatMap(workflow -> workflowService.update(workflow, userService.getSystemUser()))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Post-processing job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Post-processing job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<WorkflowDto> prepareWorkflow(WorkflowDto workflow) {
        workflow.setEndedAt(Instant.now());
        workflow.setExecutionStatus(Workflow.WorkflowExecutionStatus.COMPLETED);
        return Mono.just(workflow);
    }

    private Mono<WorkflowDto> prepareCycle(WorkflowDto workflow, String cycleId) {
        Workflow.Cycle cycle = WorkflowUtil.findCycle(workflow, cycleId);
        cycle.setFinishedAt(Instant.now());
        cycle.setExecutionStatus(Workflow.CycleExecutionStatus.COMPLETED);
        return Mono.just(workflow);
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowDto workflow) {
        return Mono.just(Map.of());
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Post-processing job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
