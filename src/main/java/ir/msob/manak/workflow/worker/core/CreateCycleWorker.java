package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.worker.WorkerUtils;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class CreateCycleWorker {

    private static final Logger logger = LoggerFactory.getLogger(CreateCycleWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;
    private final IdService idService;

    /**
     * Main worker entry point for "create-cycle" jobs.
     * <p>
     * This method is fully reactive:
     * - Does NOT call subscribe() directly to avoid blocking the reactive pipeline.
     * - On success, records a SUCCESS worker history and completes the Camunda job with result variables.
     * - On error, records an ERROR worker history, completes the Camunda job with an error result,
     * and then rethrows the exception to propagate the error.
     */
    @JobWorker(type = "create-cycle", autoComplete = false)
    public void execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableUtils.safeString(vars.get(WORKFLOW_ID_KEY));

        // Log the start of the job execution
        logger.info("Starting 'create-cycle' job. jobKey={} workflowId={}", job.getKey(), workflowId);

        Workflow.Cycle cycle = prepareCycle();

        workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new IllegalStateException("Workflow not found: " + workflowId)))
                .flatMap(workflowDto -> {
                    workflowDto.getCycles().add(cycle); // Add the new cycle
                    return workflowService.update(workflowDto, userService.getSystemUser());
                })
                .doOnSuccess(saved -> logger.info("Cycle created and workflow saved successfully. workflowId={}", saved.getId()))
                .flatMap(savedWorkflow -> recordWorkerHistory(savedWorkflow.getId())
                        .then(Mono.just(cycle)))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Job execution failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex))
                .subscribe();
    }

    private Workflow.Cycle prepareCycle() {
        return Workflow.Cycle.builder()
                .id(idService.newId())
                .executionStatus(Workflow.CycleExecutionStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .build();
    }

    private Mono<Void> recordWorkerHistory(String workflowId) {
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.SUCCESS, null);
    }

    private Mono<Map<String, Object>> prepareResult(Workflow.Cycle cycle) {
        return Mono.just(Map.of(
                CYCLE_ID_KEY, cycle.getId(),
                CYCLE_EXECUTION_STATUS_KEY, Workflow.CycleExecutionStatus.IN_PROGRESS
        ));
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Create cycle job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, WorkerUtils.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}

