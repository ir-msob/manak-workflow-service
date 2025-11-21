package ir.msob.manak.workflow.client;

import ir.msob.manak.domain.model.vectorhub.document.DocumentOverview;
import ir.msob.manak.domain.model.vectorhub.repository.RepositoryOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class VectorHubClient {

    private final WebClient webClient;

    public Mono<DocumentOverview> getDocumentOverview(String id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.host("vectorhub")
                        .path("/api/v1/document/{id}")
                        .build(id))
                .retrieve()
                .bodyToMono(DocumentOverview.class);
    }

    public Mono<RepositoryOverview> getRepositoryOverview(String id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.host("vectorhub")
                        .path("/api/v1/repository/{id}")
                        .build(id))
                .retrieve()
                .bodyToMono(RepositoryOverview.class);
    }
}
