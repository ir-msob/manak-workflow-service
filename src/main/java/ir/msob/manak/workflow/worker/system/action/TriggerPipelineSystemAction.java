package ir.msob.manak.workflow.worker.system.action;


import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.rms.dto.PipelineResult;
import ir.msob.manak.domain.model.rms.dto.PipelineSpec;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.service.toolhub.ToolInvoker;
import ir.msob.manak.workflow.worker.common.ToolHandler;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.domain.model.rms.RmsConstants.*;

@Component
@RequiredArgsConstructor
public class TriggerPipelineSystemAction implements SystemActionHandler, ToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(TriggerPipelineSystemAction.class);

    @Getter
    private final ToolInvoker toolInvoker;
    @Getter
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        Map<String, Object> toolInput = Map.of(
                REPOSITORY_ID_KEY, VariableUtils.safeString(params.get(REPOSITORY_ID_KEY)),
                PIPELINE_SPEC_KEY, objectMapper.convertValue(params.get(PIPELINE_SPEC_KEY), PipelineSpec.class)
        );
        logger.info("TriggerPipeline started. toolInput={}", toolInput);

        return invoke("Repository:TriggerPipeline:1.0.0", toolInput)
                .doOnError(ex -> logger.error("TriggerPipeline failed: {}", ex.getMessage(), ex));
    }


    @Override
    public Mono<Map<String, Object>> prepareSuccessResult(InvokeResponse invokeResponse) {
        return castResult(invokeResponse)
                .map(res -> Map.of(
                        PIPELINE_EXECUTION_STATUS_KEY, res.getStatus(),
                        PIPELINE_EXECUTION_RESULT_KEY, res.getMessage()
                ));
    }


    @SneakyThrows
    private Mono<PipelineResult> castResult(InvokeResponse response) {
        return response.getResult() == null
                ? Mono.empty()
                : Mono.just(objectMapper.convertValue(response.getResult(), PipelineResult.class));
    }
}