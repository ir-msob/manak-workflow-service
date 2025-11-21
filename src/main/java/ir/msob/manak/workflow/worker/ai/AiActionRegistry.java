package ir.msob.manak.workflow.worker.ai;

import ir.msob.manak.workflow.worker.common.ActionHandler;
import ir.msob.manak.workflow.worker.common.ActionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiActionRegistry implements ActionRegistry {

    private final Map<String, AiActionHandler> actions;

    @Override
    public ActionHandler getActionHandler(String action) {
        ActionHandler actionHandler = actions.get(action);
        if (actionHandler == null) {
            throw new IllegalArgumentException("Action [" + action + "] not found");
        }
        return actionHandler;
    }
}
