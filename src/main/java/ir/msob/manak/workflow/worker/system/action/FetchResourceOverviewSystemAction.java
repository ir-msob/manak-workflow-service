package ir.msob.manak.workflow.worker.system.action;

import ir.msob.manak.domain.model.vectorhub.document.DocumentOverview;
import ir.msob.manak.domain.model.vectorhub.repository.RepositoryOverview;
import ir.msob.manak.domain.model.workflow.dto.ResourceOverview;
import ir.msob.manak.workflow.client.VectorHubClient;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.RESOURCE_OVERVIEWS_KEY;

@Component
@RequiredArgsConstructor
public class FetchResourceOverviewSystemAction implements SystemActionHandler {

    private final VectorHubClient vectorHubClient;

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<ResourceOverview> resources = VariableHelper.safeList(params.get(RESOURCE_OVERVIEWS_KEY), ResourceOverview.class);

        return Flux.fromIterable(resources)
                .flatMap(this::fetchOverviewIfNeeded)
                .collectList()
                .flatMap(this::prepareResult);
    }

    private Mono<Map<String, Object>> prepareResult(List<ResourceOverview> resourceOverviews) {
        return Mono.just(Map.of(RESOURCE_OVERVIEWS_KEY, resourceOverviews));
    }

    private Mono<ResourceOverview> fetchOverviewIfNeeded(ResourceOverview resourceOverview) {
        if (Strings.isNotBlank(resourceOverview.getOverview())) {
            return Mono.just(resourceOverview);
        }

        Mono<String> fetchMono = switch (resourceOverview.getType()) {
            case DOCUMENT -> vectorHubClient.getDocumentOverview(resourceOverview.getId())
                    .map(DocumentOverview::getOverview);

            case REPOSITORY -> vectorHubClient.getRepositoryOverview(resourceOverview.getId())
                    .map(RepositoryOverview::getOverview);
        };

        return fetchMono
                .defaultIfEmpty("")
                .map(overview -> {
                    resourceOverview.setOverview(overview);
                    return resourceOverview;
                });
    }
}
