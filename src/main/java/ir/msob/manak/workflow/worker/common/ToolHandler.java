package ir.msob.manak.workflow.worker.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.service.toolhub.ToolInvoker;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.domain.model.worker.Constants.WORKER_EXECUTION_ERROR_KEY;
import static ir.msob.manak.domain.model.worker.Constants.WORKER_EXECUTION_STATUS_KEY;

public interface ToolHandler {

    ToolInvoker getToolInvoker();

    ObjectMapper getObjectMapper();

    Mono<Map<String, Object>> prepareSuccessResult(InvokeResponse invokeResponse);


    default Mono<Map<String, Object>> invoke(String toolId, Map<String, Object> params) {
        return getToolInvoker().invokeReactive(toolId, params)
                .flatMap(this::handleInvokeResponse);
    }

    /**
     * Decide success or failure based on InvokeResponse
     */
    private Mono<Map<String, Object>> handleInvokeResponse(InvokeResponse response) {
        if (response.getError() != null) {
            return Mono.just(prepareErrorResult(response));
        }
        return this.prepareSuccessResult(response);
    }


    /**
     * Prepare structured error result when InvokeResponse has an error
     */
    private Map<String, Object> prepareErrorResult(InvokeResponse response) {
        return Map.of(
                WORKER_EXECUTION_STATUS_KEY, WorkerExecutionStatus.ERROR,
                WORKER_EXECUTION_ERROR_KEY, response.getError().getMessage()
        );
    }

}
