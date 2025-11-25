package ir.msob.manak.workflow.worker.ai.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.model.workflow.dto.DataRequirement;
import ir.msob.manak.domain.model.workflow.dto.ResourceContent;
import ir.msob.manak.domain.model.workflow.dto.ResourceOverview;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.domain.service.client.ChatClient;
import ir.msob.manak.workflow.worker.ai.AiActionHandler;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
public class DataRequirementAiAction extends AiActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DataRequirementAiAction.class);

    private final ObjectMapper objectMapper;
    private final WorkflowService workflowService;
    private final UserService userService;

    public DataRequirementAiAction(ChatClient chatClient,
                                   ObjectMapper objectMapper,
                                   WorkflowService workflowService,
                                   UserService userService) {
        super(chatClient);
        this.objectMapper = objectMapper;
        this.workflowService = workflowService;
        this.userService = userService;
    }

    @Override
    @SneakyThrows
    protected Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params) {
        String workflowId = VariableUtils.safeString(params.get(WORKFLOW_ID_KEY));
        logger.info("DataRequirement action started. workflowId={}", workflowId);

        DataRequirement dataRequirement;
        try {
            dataRequirement = objectMapper.readValue(aiResponse, DataRequirement.class);
        } catch (Exception ex) {
            logger.error("Error parsing AI response into DataRequirement. workflowId={}", workflowId, ex);
            return Mono.error(ex);
        }

        return updateWorkflowContext(workflowId, dataRequirement)
                .doOnSuccess(wf -> logger.info("Context updated successfully. workflowId={}", workflowId))
                .doOnError(ex -> logger.error("Context update failed. workflowId={}", workflowId, ex))
                .thenReturn(Map.of());
    }

    private Mono<WorkflowDto> updateWorkflowContext(String workflowId, DataRequirement dataRequirement) {

        return workflowService.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Workflow not found. workflowId=" + workflowId)))
                .flatMap(workflow -> {

                    Map<String, Object> ctx = workflow.getContext();
                    if (ctx == null) {
                        ctx = new HashMap<>();
                        workflow.setContext(ctx);
                    }

                    List<ResourceContent> existingContents =
                            VariableUtils.safeList(ctx.get(RESOURCE_CONTENTS_KEY));

                    List<ResourceOverview> existingOverviews =
                            VariableUtils.safeList(ctx.get(RESOURCE_OVERVIEWS_KEY));

                    if (existingContents == null) {
                        existingContents = new ArrayList<>();
                        ctx.put(RESOURCE_CONTENTS_KEY, existingContents);
                    }

                    if (existingOverviews == null) {
                        existingOverviews = new ArrayList<>();
                        ctx.put(RESOURCE_OVERVIEWS_KEY, existingOverviews);
                    }

                    addMissingContent(existingContents, dataRequirement.getResourceContents());
                    addMissingOverview(existingOverviews, dataRequirement.getResourceOverviews());

                    return workflowService.update(workflow, userService.getSystemUser());
                });
    }

    private void addMissingContent(List<ResourceContent> target,
                                   List<ResourceContent> incoming) {
        if (incoming == null || incoming.isEmpty())
            return;

        for (ResourceContent item : incoming) {
            boolean exists = target.stream()
                    .anyMatch(ex -> Objects.equals(ex.getId(), item.getId()));

            if (!exists) {
                target.add(item);
            }
        }
    }

    private void addMissingOverview(List<ResourceOverview> target,
                                    List<ResourceOverview> incoming) {
        if (incoming == null || incoming.isEmpty())
            return;

        for (ResourceOverview item : incoming) {
            boolean exists = target.stream()
                    .anyMatch(ex -> Objects.equals(ex.getId(), item.getId()));

            if (!exists) {
                target.add(item);
            }
        }
    }

}
