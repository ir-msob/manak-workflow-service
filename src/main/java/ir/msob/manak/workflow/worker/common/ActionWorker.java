package ir.msob.manak.workflow.worker.common;

import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.worker.WorkerUtils;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static ir.msob.manak.workflow.worker.Constants.*;

@RequiredArgsConstructor
public abstract class ActionWorker {

    private static final Logger logger = LoggerFactory.getLogger(ActionWorker.class);

    private final CamundaService camundaService;
    private final WorkflowService workflowService;

    protected abstract ActionRegistry getActionRegistry();

    protected Mono<Void> execute(final ActivatedJob job) {

        Map<String, Object> vars = job.getVariablesAsMap();
        String workflowId = VariableUtils.safeString(vars.get(WORKFLOW_ID_KEY));
        String jobKey = String.valueOf(job.getKey());

        logger.info("ActionWorker started. jobKey={} workflowId={}", jobKey, workflowId);

        String action = Optional.ofNullable(VariableUtils.safeString(vars.get(ACTION_KEY)))
                .orElseThrow(() -> new IllegalArgumentException("Missing action key"));

        logger.info("Action resolved. jobKey={} action={}", jobKey, action);

        Map<String, Object> params = Optional.ofNullable(VariableUtils.safeMapStringObject(vars.get(PARAMS_KEY)))
                .orElseThrow(() -> new IllegalArgumentException("Missing params key"));

        logger.info("Parameters received. jobKey={} count={} params={}", jobKey, params.size(), params);

        ActionHandler actionHandler = getActionRegistry().getActionHandler(action);

        return actionHandler.execute(params)
                .flatMap(result -> {
                    logger.info("Action completed successfully. jobKey={} workflowId={}", jobKey, workflowId);
                    return recordWorkerHistory(workflowId).thenReturn(result);
                })
                .flatMap(result ->
                        camundaService.complete(job, result)
                )
                .onErrorResume(ex -> {
                    logger.error("Action execution FAILED. jobKey={} workflowId={} msg={}", jobKey, workflowId, ex.getMessage(), ex);
                    return handleErrorAndReThrow(job, workflowId, ex)
                            .then(Mono.error(ex));
                });
    }

    private Mono<Void> recordWorkerHistory(String workflowId) {
        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.SUCCESS, null);
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String workflowId, Throwable ex) {
        String jobKey = String.valueOf(job.getKey());
        String msg = String.format("Worker execution failed. jobKey=%s workflowId=%s error=%s",
                jobKey,
                workflowId,
                ex.getMessage()
        );

        return workflowService.recordWorkerHistory(workflowId, WorkerExecutionStatus.ERROR, msg)
                .then(camundaService.complete(job, WorkerUtils.prepareErrorResult(ex.getMessage())))
                .then(Mono.error(ex));
    }
}
