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
import ir.msob.manak.workflow.camunda.CamundaService;
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
public class CycleCompleteSuccessWorker {

    private static final Logger logger = LoggerFactory.getLogger(CycleCompleteSuccessWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    /**
     * Worker entry point for "cycle-complete-success" jobs.
     * <p>
     * This method is fully reactive:
     * - Does NOT call subscribe() directly to avoid blocking the reactive pipeline.
     * - Marks the specified cycle as COMPLETED.
     * - Updates the workflow, prepares the result, and completes the Camunda job.
     * - On error, records an ERROR worker history, completes the Camunda job with error details,
     * and rethrows the exception.
     */
    @JobWorker(type = "cycle-complete-success", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableUtils.safeString(vars.get(WORKFLOW_ID_KEY));
        String cycleId = VariableUtils.safeString(vars.get(CYCLE_ID_KEY));

        // Log the start of the job execution
        logger.info("Starting 'cycle-complete-success' job. jobKey={} workflowId={} cycleId={}", job.getKey(), workflowId, cycleId);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(workflowDto -> prepareCycle(workflowDto, cycleId))
                .flatMap(workflow -> workflowService.update(workflow, userService.getSystemUser()))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Cycle-complete-success job completed successfully. jobKey={} cycleId={}", job.getKey(), cycleId))
                .doOnError(ex -> logger.error("Cycle-complete-success job failed. jobKey={} cycleId={} error={}", job.getKey(), cycleId, ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Mono<WorkflowDto> prepareCycle(WorkflowDto workflow, String cycleId) {
        Workflow.Cycle cycle = WorkflowUtil.findCycle(workflow, cycleId);
        cycle.setFinishedAt(Instant.now());
        cycle.setExecutionStatus(Workflow.CycleExecutionStatus.COMPLETED);
        return Mono.just(workflow);
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowDto workflow) {
        // Currently no result variables needed, returning empty map
        return Mono.just(Map.of());
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Cycle-complete-success job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, WorkerUtils.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
