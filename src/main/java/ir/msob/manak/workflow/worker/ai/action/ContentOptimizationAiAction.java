package ir.msob.manak.workflow.worker.ai.action;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.service.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.OPTIMIZED_CONTENT_KEY;
import static ir.msob.manak.workflow.worker.Constants.REQUEST_ID_KEY;

@Component
public class ContentOptimizationAiAction extends AiActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ContentOptimizationAiAction.class);

    public ContentOptimizationAiAction(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    protected Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params) {
        String requestId = VariableUtils.safeString(params.get(REQUEST_ID_KEY));

        logger.info("ContentOptimization AI action started. requestId={}", requestId);

        return Mono.just(Map.of(
                OPTIMIZED_CONTENT_KEY, aiResponse
        ));
    }
}
