package ir.msob.manak.workflow.worker.system.action;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.ResourceContent;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ir.msob.manak.workflow.worker.Constants.NEW_RESOURCE_CONTENTS_KEY;
import static ir.msob.manak.workflow.worker.Constants.RESOURCE_CONTENTS_KEY;

@Component
@RequiredArgsConstructor
public class ResourceContentMergeSystemAction implements SystemActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResourceContentMergeSystemAction.class);

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<ResourceContent> existingContents = VariableUtils.safeList(params.get(RESOURCE_CONTENTS_KEY));
        List<ResourceContent> newContents = VariableUtils.safeList(params.get(NEW_RESOURCE_CONTENTS_KEY));

        logger.info("ResourceContentMerge execution started. {} existingContents and {} newContents to process.", existingContents.size(), newContents.size());


        mergeUnique(existingContents, newContents);

        return Mono.just(Map.of(RESOURCE_CONTENTS_KEY, existingContents));
    }

    private void mergeUnique(List<ResourceContent> existingContents, List<ResourceContent> newContents) {
        for (ResourceContent newContent : newContents) {
            if (newContent == null || newContent.getId() == null) {
                logger.warn("Skipping invalid newContent: {}", newContent);
                continue;
            }

            Optional<ResourceContent> existingContentOpt = existingContents.stream()
                    .filter(ec -> ec.getId() != null && ec.getId().equals(newContent.getId()))
                    .findFirst();

            existingContentOpt.ifPresentOrElse(existingContent -> {
                if (Strings.isBlank(existingContent.getContent())) {
                    existingContent.setContent(newContent.getContent());
                }
            }, () -> existingContents.add(newContent));
        }
    }
}
