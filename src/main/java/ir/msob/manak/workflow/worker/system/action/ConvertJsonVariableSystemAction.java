package ir.msob.manak.workflow.worker.system.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.workflow.worker.system.SystemActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static ir.msob.manak.workflow.worker.Constants.*;

@Component
@RequiredArgsConstructor
public class ConvertJsonVariableSystemAction implements SystemActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConvertJsonVariableSystemAction.class);

    private final ObjectMapper objectMapper;


    @Override
    public Mono<Map<String, Object>> execute(Map<String, Object> params) {
        String inputPlaceholder = VariableUtils.safeString(params.get(CONVERT_VARIABLE_INPUT_PLACEHOLDER_KEY));
        String targetClassName = VariableUtils.safeString(params.get(CONVERT_VARIABLE_TARGET_CLASS_KEY));
        Object raw = params.get(inputPlaceholder);

        if (raw == null) {
            logger.warn("ConvertVariable: input is null. placeholder={}", inputPlaceholder);
            return Mono.just(Map.of(CONVERT_VARIABLE_OUTPUT_PLACEHOLDER_KEY, ""));
        }

        String json;
        if (raw instanceof String) {
            json = (String) raw;
        } else {
            try {
                json = objectMapper.writeValueAsString(raw);
            } catch (Exception e) {
                logger.error("ConvertVariable: failed to serialize non-string input. placeholder={}, error={}", inputPlaceholder, e);
                return Mono.just(Map.of(CONVERT_VARIABLE_OUTPUT_PLACEHOLDER_KEY, ""));
            }
        }

        Object result;
        try {
            if (targetClassName == null || targetClassName.isBlank()) {
                json = json.trim();
                if (json.startsWith("[")) {
                    result = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
                    });
                } else {
                    result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
                    });
                }
            } else {
                Class<?> targetClazz = Class.forName(targetClassName);
                result = objectMapper.readValue(json, targetClazz);
            }
        } catch (ClassNotFoundException cnf) {
            logger.error("ConvertVariable: target class not found: {}", targetClassName, cnf);
            result = null;
        } catch (Exception ex) {
            logger.error("ConvertVariable: failed to convert JSON. targetClass={}, error={}", targetClassName, ex);
            result = null;
        }

        if (result != null)
            return Mono.just(Map.of(CONVERT_VARIABLE_OUTPUT_PLACEHOLDER_KEY, result));
        else
            return Mono.just(Map.of(CONVERT_VARIABLE_OUTPUT_PLACEHOLDER_KEY, ""));
    }
}
