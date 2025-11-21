package ir.msob.manak.workflow.worker.common;


public interface ActionRegistry {
    ActionHandler getActionHandler(String action);
}
