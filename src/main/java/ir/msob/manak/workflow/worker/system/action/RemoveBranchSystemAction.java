package ir.msob.manak.workflow.worker.system.action;


import ir.msob.manak.domain.model.workflow.dto.RepositoryBranch;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.REPOSITORY_BRANCHES_KEY;

@Component
@RequiredArgsConstructor
public class RemoveBranchSystemAction implements SystemActionHandler {

    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<RepositoryBranch> repositoryBranchLes = VariableHelper.safeList(params.get(REPOSITORY_BRANCHES_KEY), RepositoryBranch.class);

        return null;
    }

    private Mono<Map<String, Object>> prepareResult() {
        return Mono.just(Map.of());
    }


}
