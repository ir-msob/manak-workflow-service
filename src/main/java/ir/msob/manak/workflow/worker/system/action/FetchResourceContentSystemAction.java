package ir.msob.manak.workflow.worker.system.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.rms.dto.FileContent;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.ResourceContent;
import ir.msob.manak.domain.service.toolhub.ToolInvoker;
import ir.msob.manak.workflow.worker.common.ToolHandler;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.domain.model.rms.RmsConstants.*;
import static ir.msob.manak.workflow.worker.Constants.CONTENT_KEY;
import static ir.msob.manak.workflow.worker.Constants.RESOURCE_CONTENTS_KEY;

@Component
@RequiredArgsConstructor
public class FetchResourceContentSystemAction implements SystemActionHandler, ToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(FetchResourceContentSystemAction.class);

    @Getter
    private final ToolInvoker toolInvoker;

    @Getter
    private final ObjectMapper objectMapper;


    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<ResourceContent> resources =
                VariableUtils.safeList(params.get(RESOURCE_CONTENTS_KEY));

        logger.info("FetchResourceContent execution started. {} resources to process.", resources.size());

        return Flux.fromIterable(resources)
                .flatMap(resourceContent -> fetchContentIfNeeded(resourceContent, params))
                .collectList()
                .map(updated -> Map.of(RESOURCE_CONTENTS_KEY, updated));
    }


    private Mono<ResourceContent> fetchContentIfNeeded(ResourceContent resourceContent,
                                                       Map<String, Object> params) {

        // if content already exists → return as is
        if (Strings.isNotBlank(resourceContent.getContent())) {
            logger.debug("Content exists. Skipping fetch for: {}", resourceContent.getPath());
            return Mono.just(resourceContent);
        }

        // build tool input
        Map<String, Object> toolInput = Map.of(
                REPOSITORY_ID_KEY, VariableUtils.safeString(params.get(REPOSITORY_ID_KEY)),
                FILE_PATH_KEY, VariableUtils.safeString(resourceContent.getPath()),
                BRANCH_KEY, VariableUtils.safeString(params.get(BRANCH_KEY))
        );

        logger.info("Fetching content for file: {}", resourceContent.getPath());

        return invoke("Repository:GetFileContent:1.0.0", toolInput)
                .flatMap(resultMap -> {
                    resourceContent.setContent(
                            VariableUtils.safeString(resultMap.get(CONTENT_KEY))
                    );
                    logger.debug("Content fetched for file: {}", resourceContent.getPath());
                    return Mono.just(resourceContent);
                })
                .doOnError(ex -> logger.error(
                        "Failed to fetch content for file {}: {}",
                        resourceContent.getPath(),
                        ex.getMessage(), ex
                ));
    }


    @Override
    public Mono<Map<String, Object>> prepareSuccessResult(InvokeResponse invokeResponse) {
        return castResult(invokeResponse)
                .map(res -> Map.of(CONTENT_KEY, res.getContent()));
    }


    @SneakyThrows
    private Mono<FileContent> castResult(InvokeResponse response) {
        return response.getResult() == null
                ? Mono.empty()
                : Mono.just(objectMapper.convertValue(response.getResult(), FileContent.class));
    }

}
