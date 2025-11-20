package ir.msob.manak.workflow.worker.util;

import ir.msob.jima.core.commons.exception.datanotfound.DataNotFoundException;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;

public class WorkflowUtil {
    private WorkflowUtil() {
    }

    public static WorkflowSpecification.StageSpec findStageSpecByKey(Workflow workflowDto, String stageKey) {
        return workflowDto.getSpecification().getStages().stream()
                .filter(ss -> ss.getKey().equalsIgnoreCase(stageKey))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Stage not found: " + stageKey));
    }

    public static Workflow.Cycle findCycle(Workflow workflowDto, String cycleId) {
        return workflowDto.getCycles().stream()
                .filter(cycle -> cycle.getId().equalsIgnoreCase(cycleId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Cycle not found: " + cycleId));
    }


    public static WorkflowSpecification.StageSpec findFirstStageSpec(Workflow workflowDto) {
        return workflowDto.getSpecification().getStages().stream()
                .filter(WorkflowSpecification.StageSpec::isFirstStage)
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("First stage not found in workflow specification"));
    }

    public static Workflow.StageHistory findStageHistory(Workflow workflowDto, String cycleId, String stageHistoryId) {
        return findCycle(workflowDto, cycleId)
                .getStagesHistory()
                .stream()
                .filter(stageHistory -> stageHistory.getId().equalsIgnoreCase(stageHistoryId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Stage history not found: " + stageHistoryId));
    }
}
