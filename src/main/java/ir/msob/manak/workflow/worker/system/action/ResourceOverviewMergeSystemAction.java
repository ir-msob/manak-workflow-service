package ir.msob.manak.workflow.worker.system.action;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.ResourceOverview;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ir.msob.manak.workflow.worker.Constants.NEW_RESOURCE_OVERVIEWS_KEY;
import static ir.msob.manak.workflow.worker.Constants.RESOURCE_OVERVIEWS_KEY;


@Component
@RequiredArgsConstructor
public class ResourceOverviewMergeSystemAction implements SystemActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResourceOverviewMergeSystemAction.class);

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<ResourceOverview> existingOverviews = VariableUtils.safeList(params.get(RESOURCE_OVERVIEWS_KEY));
        List<ResourceOverview> newOverviews = VariableUtils.safeList(params.get(NEW_RESOURCE_OVERVIEWS_KEY));

        logger.info("ResourceOverviewMerge execution started. {} existingOverviews and {} newOverviews to process.", existingOverviews.size(), newOverviews.size());

        mergeUnique(existingOverviews, newOverviews);

        return Mono.just(Map.of(RESOURCE_OVERVIEWS_KEY, existingOverviews));
    }

    private void mergeUnique(List<ResourceOverview> existingOverviews, List<ResourceOverview> newOverviews) {
        for (ResourceOverview newOverview : newOverviews) {
            if (newOverview == null || newOverview.getId() == null) {
                logger.warn("Skipping invalid newOverview: {}", newOverview);
                continue;
            }

            Optional<ResourceOverview> existingOverviewOpt = existingOverviews.stream()
                    .filter(ec -> ec.getId() != null && ec.getId().equals(newOverview.getId()))
                    .findFirst();

            existingOverviewOpt.ifPresentOrElse(existingOverview -> {
                if (Strings.isBlank(existingOverview.getOverview())) {
                    existingOverview.setOverview(newOverview.getOverview());
                }
            }, () -> existingOverviews.add(newOverview));
        }
    }
}
