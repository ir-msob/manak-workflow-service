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
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.WORKFLOW_ID_KEY;

@Component
@RequiredArgsConstructor
public class WorkflowCompleteSuccessWorker {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowCompleteSuccessWorker.class);

    private final WorkflowService workflowService;
    private final UserService userService;
    private final CamundaService camundaService;

    /**
     * Executes the workflow completion post-processing.
     * Marks workflow as COMPLETED and updates the DB.
     * Sends empty result map back to Camunda process.
     */
    @JobWorker(type = "workflow-complete-success", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableHelper.safeString(vars.get(WORKFLOW_ID_KEY));

        logger.info("Starting workflow completion job. jobKey={}, workflowId={}", job.getKey(), workflowId);

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new DataNotFoundException("Workflow not found: " + workflowId)))
                .flatMap(this::prepareWorkflow)
                .flatMap(workflow -> workflowService.update(workflow, userService.getSystemUser()))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Workflow completion job finished successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Workflow completion job failed. jobKey={}, error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, workflowId, ex));
    }

    /**
     * Marks the workflow as COMPLETED and sets endedAt timestamp.
     */
    private Mono<WorkflowDto> prepareWorkflow(WorkflowDto workflow) {
        workflow.setEndedAt(Instant.now());
        workflow.setExecutionStatus(Workflow.WorkflowExecutionStatus.COMPLETED);
        logger.debug("Workflow marked as COMPLETED. workflowId={}", workflow.getId());
        return Mono.just(workflow);
    }

    /**
     * Prepares result map for Camunda.
     * For workflow completion, no process variables are returned.
     */
    private Mono<Map<String, Object>> prepareResult(WorkflowDto workflow) {
        return Mono.just(Map.of());
    }

    /**
     * Handles errors: records them in workflow history, sends error to Camunda, then rethrows.
     */
    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String errorMessage = "Workflow completion job failed. jobKey=" + job.getKey() + " error=" + ex.getMessage();
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, errorMessage)
                .then(camundaService.complete(job, VariableHelper.prepareErrorResult(errorMessage)))
                .then(Mono.error(ex));
    }
}
