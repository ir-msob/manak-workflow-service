package ir.msob.manak.workflow.worker.ai.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.DiffPatchData;
import ir.msob.manak.domain.service.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.REPOSITORY_DIFF_PATCHES_KEY;
import static ir.msob.manak.workflow.worker.Constants.REQUEST_ID_KEY;

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
        String requestId = VariableUtils.safeString(params.get(REQUEST_ID_KEY));


        logger.debug("Starting PatchGenerationAiAction. requestId={}", requestId);
        return parseAiResponse(aiResponse)
                .doOnNext(parsed ->
                        logger.info("AI response successfully parsed. requestId={}", requestId)
                )
                .flatMap(this::prepareMap)
                .doOnError(e ->
                        logger.error("PatchGenerationAiAction failed. requestId={}, error={}",
                                requestId, e.getMessage())
                )
                .doOnSuccess(res ->
                        logger.debug("PatchGenerationAiAction completed successfully. requestId={}",
                                requestId)
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
