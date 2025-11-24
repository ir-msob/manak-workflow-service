package ir.msob.manak.workflow.worker.system.action;


import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.RepositoryPullRequest;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.REPOSITORY_PULL_REQUESTS_KEY;

@Component
@RequiredArgsConstructor
public class CreatePullRequestSystemAction implements SystemActionHandler {


    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<RepositoryPullRequest> repositoryPullRequests = VariableUtils.safeList(params.get(REPOSITORY_PULL_REQUESTS_KEY));

        return null;
    }

    private Mono<Map<String, Object>> prepareResult() {
        return Mono.just(Map.of());
    }


}
