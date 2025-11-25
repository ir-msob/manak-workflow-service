package ir.msob.manak.workflow.worker.system.action;


import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.rms.dto.BranchRef;
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

import static ir.msob.manak.domain.model.rms.RmsConstants.BRANCH_KEY;
import static ir.msob.manak.domain.model.rms.RmsConstants.REPOSITORY_ID_KEY;

@Component
@RequiredArgsConstructor
public class DeleteBranchSystemAction implements SystemActionHandler, ToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteBranchSystemAction.class);

    @Getter
    private final ToolInvoker toolInvoker;
    @Getter
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        Map<String, Object> toolInput = Map.of(
                REPOSITORY_ID_KEY, VariableUtils.safeString(params.get(REPOSITORY_ID_KEY)),
                BRANCH_KEY, VariableUtils.safeString(params.get(BRANCH_KEY))
        );
        logger.info("DeleteBranch started. toolInput={}", toolInput);

        return invoke("Repository:DeleteBranch:1.0.0", toolInput)
                .doOnError(ex -> logger.error("DeleteBranch failed: {}", ex.getMessage(), ex));
    }


    @Override
    public Mono<Map<String, Object>> prepareSuccessResult(InvokeResponse invokeResponse) {
        return castResult(invokeResponse)
                .map(res -> Map.of(
                ));
    }


    @SneakyThrows
    private Mono<BranchRef> castResult(InvokeResponse response) {
        return response.getResult() == null
                ? Mono.empty()
                : Mono.just(objectMapper.convertValue(response.getResult(), BranchRef.class));
    }
}