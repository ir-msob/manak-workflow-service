package ir.msob.manak.workflow.worker.system.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.rms.dto.ScmResult;
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
public class ApplyPatchSystemAction implements SystemActionHandler, ToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApplyPatchSystemAction.class);

    @Getter
    private final ToolInvoker toolInvoker;
    @Getter
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        Map<String, Object> toolInput = Map.of(
                REPOSITORY_ID_KEY, VariableUtils.safeString(params.get(REPOSITORY_ID_KEY)),
                BRANCH_KEY, VariableUtils.safeString(params.get(BRANCH_KEY)),
                PATCH_KEY, VariableUtils.safeString(params.get(PATCH_KEY)),
                COMMIT_MESSAGE_KEY, VariableUtils.safeString(params.get(COMMIT_MESSAGE_KEY))
        );
        logger.info("ApplyPatch started. toolInput={}", toolInput);

        return invoke("Repository:ApplyPatch:1.0.0", toolInput)
                .doOnError(ex -> logger.error("ApplyPatch failed: {}", ex.getMessage(), ex));
    }


    @Override
    public Mono<Map<String, Object>> prepareSuccessResult(InvokeResponse invokeResponse) {
        return castResult(invokeResponse)
                .map(res -> Map.of(
                ));
    }


    @SneakyThrows
    private Mono<ScmResult> castResult(InvokeResponse response) {
        return response.getResult() == null
                ? Mono.empty()
                : Mono.just(objectMapper.convertValue(response.getResult(), ScmResult.class));
    }
}
