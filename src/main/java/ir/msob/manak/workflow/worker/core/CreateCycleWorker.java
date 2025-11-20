package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
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
public class CreateCycleWorker {

    private static final Logger logger = LoggerFactory.getLogger(CreateCycleWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;
    private final IdService idService;

    /**
     * Main worker entry point. The method is fully reactive and avoids direct subscribe() calls
     * in non-blocking contexts. On success it writes a SUCCESS worker history entry and completes
     * the Camunda job with result variables. On error it attempts to write an ERROR worker history
     * entry and complete the Camunda job with an error message, then rethrows the error.
     */
    @JobWorker(type = "create-cycle", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));

        // Log the start of the execution with job details
        logger.info("Start executing create-cycle job. jobKey={} workflowId={}", job.getKey(), workflowId);
        Workflow.Cycle cycle = prepareCycle();

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new IllegalStateException("Workflow not found: " + workflowId)))
                .flatMap(workflowDto -> {
                    workflowDto.getCycles().add(cycle);  // Add the new cycle
                    return workflowService.save(workflowDto, userService.getSystemUser())
                            .map(savedWorkflow -> savedWorkflow);
                })
                .doOnSuccess(saved -> logger.info("Cycle created and workflow saved. workflowId={}", saved.getId()))
                .flatMap(savedWorkflow -> recordWorkerHistory(savedWorkflow.getId())
                        .then(Mono.just(cycle)))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Create cycle job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Create cycle job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    private Workflow.Cycle prepareCycle() {
        return Workflow.Cycle.builder()
                .id(idService.newId())
                .executionStatus(Workflow.CycleExecutionStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .build();
    }

    private Mono<Void> recordWorkerHistory(String workflowId) {
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.SUCCESS, null);
    }

    private Mono<Map<String, Object>> prepareResult(Workflow.Cycle cycle) {
        return Mono.just(Map.of(CYCLE_ID_KEY, cycle.getId()));
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Create cycle job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
