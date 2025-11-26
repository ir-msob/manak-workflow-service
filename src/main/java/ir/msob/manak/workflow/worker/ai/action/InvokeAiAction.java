package ir.msob.manak.workflow.worker.ai.action;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.service.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
public class InvokeAiAction extends AiActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(InvokeAiAction.class);

    public InvokeAiAction(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    protected Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params) {
        String requestId = VariableUtils.safeString(params.get(REQUEST_ID_KEY));
        String aiResponsePlaceholder = VariableUtils.safeString(params.get(AI_RESPONSE_PLACEHOLDER_KEY));

        logger.info("Invoke AI action started. requestId={}", requestId);

        return Mono.just(Map.of(
                aiResponsePlaceholder, aiResponse
        ));
    }
}
