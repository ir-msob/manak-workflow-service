package ir.msob.manak.workflow.worker.action;


public interface ActionRegistry {
    ActionHandler getActionHandler(String action);
}
