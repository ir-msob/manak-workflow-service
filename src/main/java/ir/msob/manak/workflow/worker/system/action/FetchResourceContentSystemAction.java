package ir.msob.manak.workflow.worker.system.action;


import ir.msob.manak.domain.model.workflow.dto.ResourceContent;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.RESOURCE_CONTENTS_KEY;

@Component
@RequiredArgsConstructor
public class FetchResourceContentSystemAction implements SystemActionHandler {


    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<ResourceContent> resources = VariableHelper.safeList(params.get(RESOURCE_CONTENTS_KEY), ResourceContent.class);

        return Flux.fromIterable(resources)
                .flatMap(this::fetchContentIfNeeded)
                .collectList()
                .flatMap(this::prepareResult);
    }

    private Mono<Map<String, Object>> prepareResult(List<ResourceContent> resourceContents) {
        return Mono.just(Map.of(RESOURCE_CONTENTS_KEY, resourceContents));
    }

    private Mono<ResourceContent> fetchContentIfNeeded(ResourceContent resourceContent) {
        if (Strings.isNotBlank(resourceContent.getContent())) {
            return Mono.just(resourceContent);
        }

        Mono<String> fetchMono = Mono.empty();

        return fetchMono
                .defaultIfEmpty("")
                .map(overview -> {
                    resourceContent.setContent(overview);
                    return resourceContent;
                });
    }
}
