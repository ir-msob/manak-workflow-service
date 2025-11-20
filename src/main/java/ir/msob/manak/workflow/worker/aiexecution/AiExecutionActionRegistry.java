package ir.msob.manak.workflow.worker.aiexecution;

import ir.msob.manak.workflow.worker.action.ActionHandler;
import ir.msob.manak.workflow.worker.action.ActionRegistry;
import ir.msob.manak.workflow.worker.aiexecution.action.AIContentOptimizationAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiExecutionActionRegistry implements ActionRegistry {

    private final AIContentOptimizationAction aiContentOptimizationWorker;
    private final Map<String, ActionHandler> actions = Map.of(
            "AI-Content-Optimization", aiContentOptimizationWorker
    );


    @Override
    public ActionHandler getActionHandler(String action) {
        ActionHandler actionHandler = actions.get(action);
        if (actionHandler == null) {
            throw new IllegalArgumentException("Action [" + action + "] not found");
        }
        return actionHandler;
    }
}
