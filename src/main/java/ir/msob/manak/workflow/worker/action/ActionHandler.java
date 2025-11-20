package ir.msob.manak.workflow.worker.action;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface ActionHandler {

    Mono<Map<String, Object>> execute(Map<String, Object> params);
}
