package ir.msob.manak.workflow.worker.ai.action;

import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.CONTENT_KEY;
import static ir.msob.manak.workflow.worker.Constants.OPTIMIZED_CONTENT_KEY;

@Component
public class ContentOptimizationAiAction implements AiActionHandler {

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        String content = VariableHelper.safeString(params.get(CONTENT_KEY));
        return getOptimizedContent(content)
                .flatMap(this::prepareResult);
    }

    private Mono<Map<String, Object>> prepareResult(String optimizedContents) {
        return Mono.just(Map.of(OPTIMIZED_CONTENT_KEY, optimizedContents));
    }

    private Mono<String> getOptimizedContent(String content) {
        return Mono.empty();
    }
}
