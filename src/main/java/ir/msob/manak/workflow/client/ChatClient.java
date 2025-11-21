package ir.msob.manak.workflow.client;

import ir.msob.jima.core.commons.domain.DomainInfo;
import ir.msob.jima.core.commons.domain.DtoInfo;
import ir.msob.jima.crud.api.restful.client.RestUtil;
import ir.msob.manak.domain.model.chat.chat.ChatRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ChatClient {
    private final WebClient webClient;

    public Mono<String> chat(ChatRequestDto chatRequestDto) {
        DomainInfo domainInfo = DomainInfo.info.getAnnotation(chatRequestDto.getClass());
        DtoInfo dtoInfo = DtoInfo.info.getAnnotation(chatRequestDto.getClass());
        return webClient.post()
                .uri((builder) -> builder.host(dtoInfo.serviceName())
                        .path(RestUtil.uri(dtoInfo, domainInfo)).build())
                .bodyValue(chatRequestDto)
                .retrieve()
                .bodyToMono(String.class);
    }
}