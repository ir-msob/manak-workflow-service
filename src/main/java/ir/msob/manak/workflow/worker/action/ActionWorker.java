package ir.msob.manak.workflow.worker.action;

import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static ir.msob.manak.workflow.worker.Constants.ACTION_KEY;
import static ir.msob.manak.workflow.worker.Constants.PARAMS_KEY;

@RequiredArgsConstructor
public abstract class ActionWorker {
    private static final Logger logger = LoggerFactory.getLogger(ActionWorker.class);

    private final CamundaService camundaService;

    protected abstract ActionRegistry getActionRegistry();

    /**
     * Executes the action based on the parameters provided in the job.
     * If successful, completes the job with the result. If failure occurs, sends error info to the Camunda engine.
     */
    protected Mono<Void> execute(final ActivatedJob job) {
        logger.info("Start executing action job. jobKey={}", job.getKey());

        return Mono.just(job)
                .map(ActivatedJob::getVariablesAsMap)
                .flatMap(vars -> {
                    // Log action and params
                    String action = Optional.ofNullable(VariableHelper.safeString(vars.get(ACTION_KEY)))
                            .orElseThrow(() -> new IllegalArgumentException("Action key is missing"));
                    logger.info("Action to be executed: {}", action);

                    Map<String, Object> params = Optional.ofNullable(VariableHelper.safeMapStringObject(vars.get(PARAMS_KEY)))
                            .orElseThrow(() -> new IllegalArgumentException("Params key is missing"));
                    logger.info("Parameters received: {}", params);

                    // Get the action handler based on the action
                    ActionHandler actionHandler = getActionRegistry().getActionHandler(action);

                    // Execute the action and handle the Mono result
                    return actionHandler.execute(params)
                            .flatMap(result -> {
                                logger.info("Action executed successfully. jobKey={}", job.getKey());
                                return camundaService.complete(job, result);
                            })
                            .onErrorResume(ex -> {
                                logger.error("Error during AI execution for jobKey={}: {}", job.getKey(), ex.getMessage(), ex);
                                // Return error details to Camunda engine
                                return camundaService.complete(job, VariableHelper.prepareErrorResult(ex.getMessage()))
                                        .then(Mono.error(ex));
                            });
                });
    }
}
