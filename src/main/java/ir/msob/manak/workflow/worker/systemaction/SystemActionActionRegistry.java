package ir.msob.manak.workflow.worker.systemaction;

import ir.msob.manak.workflow.worker.action.ActionHandler;
import ir.msob.manak.workflow.worker.action.ActionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemActionActionRegistry implements ActionRegistry {

    private final Map<String, ActionHandler> actions = Map.of();


    @Override
    public ActionHandler getActionHandler(String action) {
        ActionHandler actionHandler = actions.get(action);
        if (actionHandler == null) {
            throw new IllegalArgumentException("Action [" + action + "] not found");
        }
        return actionHandler;
    }
}
