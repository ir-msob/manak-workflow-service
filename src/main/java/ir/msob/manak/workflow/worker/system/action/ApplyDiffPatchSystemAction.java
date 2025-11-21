package ir.msob.manak.workflow.worker.system.action;


import ir.msob.manak.domain.model.workflow.dto.RepositoryDiffPatch;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.REPOSITORY_DIFF_PATCHES_KEY;

@Component
@RequiredArgsConstructor
public class ApplyDiffPatchSystemAction implements SystemActionHandler {


    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        List<RepositoryDiffPatch> repositoryDiffPatches = VariableHelper.safeList(params.get(REPOSITORY_DIFF_PATCHES_KEY), RepositoryDiffPatch.class);

        return null;
    }

    private Mono<Map<String, Object>> prepareResult() {
        return Mono.just(Map.of());
    }


}
