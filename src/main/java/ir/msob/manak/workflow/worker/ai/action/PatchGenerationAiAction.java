package ir.msob.manak.workflow.worker.ai.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.workflow.dto.DiffPatchData;
import ir.msob.manak.workflow.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
public class PatchGenerationAiAction extends AiActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(PatchGenerationAiAction.class);

    private final ObjectMapper objectMapper;
    private final WorkflowService workflowService;

    public PatchGenerationAiAction(ChatClient chatClient,
                                   ObjectMapper objectMapper,
                                   WorkflowService workflowService) {
        super(chatClient);
        this.objectMapper = objectMapper;
        this.workflowService = workflowService;
    }

    @Override
    @SneakyThrows
    protected Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params) {
        String workflowId = VariableHelper.safeString(params.get(WORKFLOW_ID_KEY));
        String cycleId = VariableHelper.safeString(params.get(CYCLE_ID_KEY));

        logger.debug("Starting PatchGenerationAiAction. workflowId={}, cycleId={}", workflowId, cycleId);

        return parseAiResponse(aiResponse)
                .doOnNext(parsed ->
                        logger.info("AI response successfully parsed. workflowId={}, cycleId={}", workflowId, cycleId)
                )
                .flatMap(this::prepareMap)
                .flatMap(result -> workflowService.updateCycleContext(workflowId, cycleId, result)
                        .then(Mono.just(result)))
                .doOnError(e ->
                        logger.error("PatchGenerationAiAction failed. workflowId={}, cycleId={}, error={}",
                                workflowId, cycleId, e.getMessage())
                )
                .doOnSuccess(res ->
                        logger.debug("PatchGenerationAiAction completed successfully. workflowId={}, cycleId={}",
                                workflowId, cycleId)
                );
    }

    private Mono<DiffPatchData> parseAiResponse(String aiResponse) {
        try {
            DiffPatchData data = objectMapper.readValue(aiResponse, DiffPatchData.class);
            return Mono.just(data);
        } catch (Exception ex) {
            logger.warn("Failed to parse AI response into DiffPatchData. rawResponse={}", aiResponse);
            return Mono.error(ex);
        }
    }

    private Mono<Map<String, Object>> prepareMap(DiffPatchData diffPatchData) {
        return Mono.just(Map.of(
                REPOSITORY_DIFF_PATCHES_KEY, diffPatchData.getRepositoryDiffPatches()
        ));
    }
}
