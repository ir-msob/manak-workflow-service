package ir.msob.manak.workflow.worker.core;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.workflow.WorkflowService;
import ir.msob.manak.workflow.workflowspecification.WorkflowSpecificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class CreateWorkflowWorker {

    private static final Logger logger = LoggerFactory.getLogger(CreateWorkflowWorker.class);

    private final WorkflowSpecificationService workflowSpecificationService;
    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    @JobWorker(type = "create-workflow", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowSpecificationId = VariableHelper.safeString(vars.get(WORKFLOW_SPECIFICATION_ID_KEY));

        logger.info("Start executing create-workflow job. jobKey={} workflowSpecificationId={}", job.getKey(), workflowSpecificationId);

        // Holder for workflow ID to use in error handling
        AtomicReference<String> workflowIdHolder = new AtomicReference<>();

        return workflowSpecificationService.getOne(workflowSpecificationId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new IllegalStateException("WorkflowSpecification not found: " + workflowSpecificationId)))
                .map(spec -> prepareWorkflow(spec, vars))
                .flatMap(workflowDto -> workflowService.save(workflowDto, userService.getSystemUser()))
                .doOnSuccess(saved -> {
                    logger.info("Workflow saved. id={}", saved.getId());
                    workflowIdHolder.set(saved.getId());
                })
                .flatMap(savedWorkflow -> recordWorkerHistory(savedWorkflow)
                        .then(Mono.just(savedWorkflow)))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Create workflow job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Create workflow job failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowIdHolder.get(), ex));
    }

    private Mono<Void> recordWorkerHistory(WorkflowDto workflowDto) {
        return workflowService.recordWorkerHistory(workflowDto.getId(), Workflow.WorkerExecutionStatus.SUCCESS, null);
    }

    private Mono<Map<String, Object>> prepareResult(WorkflowDto workflowDto) {
        return Mono.just(Map.of(WORKFLOW_ID_KEY, workflowDto.getId()));
    }

    private WorkflowDto prepareWorkflow(WorkflowSpecificationDto spec, Map<String, Object> vars) {
        return WorkflowDto.builder()
                .specification(spec)
                .correlationId(VariableHelper.safeString(vars.get(CORRELATION_ID_KEY)))
                .executionStatus(Workflow.WorkflowExecutionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Create workflow job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, Workflow.WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
