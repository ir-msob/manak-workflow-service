package ir.msob.manak.workflow.worker.ai;

import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.chat.chat.ChatRequestDto;
import ir.msob.manak.workflow.client.ChatClient;
import ir.msob.manak.workflow.worker.common.ActionHandler;
import ir.msob.manak.workflow.worker.util.VariableHelper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import static ir.msob.manak.workflow.worker.Constants.*;

/**
 * Base class for ActionHandlers that communicate with an AI/Chat backend.
 * <p>
 * Responsibilities:
 * - Render the AI prompt template using provided variables
 * - Build the request for the AI backend
 * - Send the request and receive a response
 * - Convert the raw response into workflow-ready output via an abstract method
 */
@RequiredArgsConstructor
public abstract class AiActionHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AiActionHandler.class);

    private final ChatClient chatClient;

    /**
     * Every subclass must convert the raw AI response (plain text) into a structured Map.
     */
    protected abstract Mono<Map<String, Object>> prepareResult(String aiResponse, Map<String, Object> params);

    /**
     * Main entry point triggered by the workflow engine.
     */
    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        Objects.requireNonNull(params, "params must not be null");

        String template = VariableHelper.safeString(params.get(AI_PROMPT_TEMPLATE_KEY));
        if (template == null || template.isBlank()) {
            logger.warn("Missing or empty AI prompt template (key: {})", AI_PROMPT_TEMPLATE_KEY);
            return Mono.error(new IllegalArgumentException("AI prompt template is required"));
        }

        String prompt = renderTemplate(template, params);

        return requestChatCompletion(prompt, params)
                .flatMap(aiResponse -> prepareResult(aiResponse, params))
                .doOnError(e -> logger.error("AI action failed: {}", e.getMessage()));
    }

    /**
     * Sends a completion request to the AI client.
     * Network or client errors are wrapped to be handled consistently upstream.
     */
    private Mono<String> requestChatCompletion(String prompt, Map<String, Object> params) {
        ChatRequestDto request = buildChatRequest(prompt, params);

        logger.debug("Sending AI request (model={}, toolsCount={})",
                request.getModelSpecificationKey(),
                request.getTools() == null ? 0 : request.getTools().size());

        return chatClient.chat(request)
                .onErrorMap(ex -> {
                    logger.error("Error while calling chat client: {}", ex.getMessage());
                    return new RuntimeException("Failed to call chat service", ex);
                });
    }

    /**
     * Constructs a ChatRequestDto using safe parameter extraction utilities.
     */
    private ChatRequestDto buildChatRequest(String prompt, Map<String, Object> params) {
        return ChatRequestDto.builder()
                .modelSpecificationKey(VariableHelper.safeString(params.get(AI_MODEL_KEY)))
                .message(prompt)
                .tools(VariableHelper.safeList(params.get(AI_TOOLS_KEY), String.class))
                .build();
    }

    /**
     * Renders ${variable} placeholders inside the template string.
     * Behavior:
     * - If a variable does not exist in the map:
     * It logs a warning and keeps ${variable} unchanged.
     * - Otherwise:
     * It replaces the placeholder with the variable's value.
     */
    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }

        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);

            if (value == null) {
                logger.warn("Template variable '{}' not found in params — leaving placeholder intact", varName);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(formatPlaceholder(varName)));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String formatPlaceholder(String varName) {
        return "${" + varName + "}";
    }
}
