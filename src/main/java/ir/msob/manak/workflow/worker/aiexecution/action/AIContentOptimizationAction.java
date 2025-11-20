package ir.msob.manak.workflow.worker.aiexecution.action;

import ir.msob.manak.workflow.worker.action.ActionHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AIContentOptimizationAction implements ActionHandler {
    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        return Mono.just(Map.of());
    }
}
