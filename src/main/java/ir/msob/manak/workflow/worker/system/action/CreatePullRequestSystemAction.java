package ir.msob.manak.workflow.worker.system.action;


import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.rms.dto.PullRequestInfo;
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
public class CreatePullRequestSystemAction implements SystemActionHandler, ToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(CreatePullRequestSystemAction.class);

    @Getter
    private final ToolInvoker toolInvoker;
    @Getter
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        Map<String, Object> toolInput = Map.of(
                REPOSITORY_ID_KEY, VariableUtils.safeString(params.get(REPOSITORY_ID_KEY)),
                SOURCE_BRANCH_KEY, VariableUtils.safeString(params.get(SOURCE_BRANCH_KEY)),
                TARGET_BRANCH_KEY, VariableUtils.safeString(params.get(TARGET_BRANCH_KEY)),
                TITLE_KEY, VariableUtils.safeString(params.get(TITLE_KEY)),
                DESCRIPTION_KEY, VariableUtils.safeString(params.get(DESCRIPTION_KEY))
        );
        logger.info("CreatePullRequest started. toolInput={}", toolInput);

        return invoke("Repository:CreatePullRequest:1.0.0", toolInput)
                .doOnError(ex -> logger.error("CreatePullRequest failed: {}", ex.getMessage(), ex));
    }


    @Override
    public Mono<Map<String, Object>> prepareSuccessResult(InvokeResponse invokeResponse) {
        return castResult(invokeResponse)
                .map(res -> Map.of(
                        PULL_REQUEST_ID_KEY, res.getId(),
                        PULL_REQUEST_LINK_KEY, res.getUrl()
                ));
    }


    @SneakyThrows
    private Mono<PullRequestInfo> castResult(InvokeResponse response) {
        return response.getResult() == null
                ? Mono.empty()
                : Mono.just(objectMapper.convertValue(response.getResult(), PullRequestInfo.class));
    }
}