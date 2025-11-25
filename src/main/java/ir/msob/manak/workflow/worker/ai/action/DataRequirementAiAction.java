package ir.msob.manak.workflow.worker.ai.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.DataRequirement;
import ir.msob.manak.domain.model.workflow.dto.ResourceContent;
import ir.msob.manak.domain.model.workflow.dto.ResourceOverview;
import ir.msob.manak.domain.service.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
public class DataRequirementAiAction extends AiActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DataRequirementAiAction.class);

    private final ObjectMapper objectMapper;

    public DataRequirementAiAction(ChatClient chatClient, ObjectMapper objectMapper) {
        super(chatClient);
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    protected Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params) {

        String requestId = VariableUtils.safeString(params.get(REQUEST_ID_KEY));
        List<ResourceContent> existingContents = VariableUtils.safeList(params.get(RESOURCE_CONTENTS_KEY));
        List<ResourceOverview> existingOverviews = VariableUtils.safeList(params.get(RESOURCE_OVERVIEWS_KEY));

        logger.info("Starting DataRequirement parsing... requestId={}", requestId);

        DataRequirement dataRequirement;
        try {
            dataRequirement = objectMapper.readValue(aiResponse, DataRequirement.class);
        } catch (Exception ex) {
            logger.error("Failed to parse AI response into DataRequirement. requestId={}", requestId, ex);
            return Mono.error(ex);
        }

        updateWorkflowContext(
                existingContents,
                existingOverviews,
                dataRequirement
        );

        logger.info("DataRequirement parsed. requestId={}, contents={}, overviews={}",
                requestId, existingContents.size(), existingOverviews.size());

        return Mono.just(
                Map.of(
                        RESOURCE_CONTENTS_KEY, existingContents,
                        RESOURCE_OVERVIEWS_KEY, existingOverviews
                )
        );
    }

    private void updateWorkflowContext(List<ResourceContent> contents,
                                       List<ResourceOverview> overviews,
                                       DataRequirement dataRequirement) {
        mergeUnique(contents, dataRequirement.getResourceContents(), ResourceContent::getId);
        mergeUnique(overviews, dataRequirement.getResourceOverviews(), ResourceOverview::getId);
    }

    private <T> void mergeUnique(List<T> target, List<T> incoming, Function<T, Object> idExtractor) {

        if (incoming == null || incoming.isEmpty())
            return;

        Set<Object> existingIds = new HashSet<>(
                target.stream()
                        .map(idExtractor)
                        .filter(Objects::nonNull)
                        .toList()
        );

        incoming.stream()
                .filter(item -> !existingIds.contains(idExtractor.apply(item)))
                .forEach(target::add);
    }
}
