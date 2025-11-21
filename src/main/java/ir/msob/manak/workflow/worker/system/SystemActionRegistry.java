package ir.msob.manak.workflow.worker.system;

import ir.msob.manak.workflow.worker.action.ActionHandler;
import ir.msob.manak.workflow.worker.action.ActionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemActionRegistry implements ActionRegistry {

    private final Map<String, SystemActionHandler> actions;


    @Override
    public ActionHandler getActionHandler(String action) {
        ActionHandler actionHandler = actions.get(action);
        if (actionHandler == null) {
            throw new IllegalArgumentException("Action [" + action + "] not found");
        }
        return actionHandler;
    }
}
