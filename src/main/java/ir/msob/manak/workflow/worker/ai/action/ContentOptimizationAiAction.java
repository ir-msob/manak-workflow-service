package ir.msob.manak.workflow.worker.ai.action;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.workflow.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import ir.msob.manak.workflow.workflow.WorkflowService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
public class ContentOptimizationAiAction extends AiActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ContentOptimizationAiAction.class);
    private final WorkflowService workflowService;

    public ContentOptimizationAiAction(ChatClient chatClient, WorkflowService workflowService) {
        super(chatClient);
        this.workflowService = workflowService;
    }

    @Override
    protected Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params) {

        String workflowId = VariableUtils.safeString(params.get(WORKFLOW_ID_KEY));
        String cycleId = VariableUtils.safeString(params.get(CYCLE_ID_KEY));

        logger.info("Starting ContentOptimizationAiAction. workflowId={}, cycleId={}", workflowId, cycleId);

        Map<String, Object> resultMap = Map.of(OPTIMIZED_CONTENT_KEY, aiResponse);

        return workflowService.updateCycleContext(workflowId, cycleId, resultMap)
                .doOnSuccess(v -> logger.info("Cycle context updated successfully. workflowId={}, cycleId={}", workflowId, cycleId))
                .doOnError(ex -> logger.error("Failed to update cycle context. workflowId={}, cycleId={}", workflowId, cycleId, ex))
                .then(Mono.just(resultMap));
    }
}
