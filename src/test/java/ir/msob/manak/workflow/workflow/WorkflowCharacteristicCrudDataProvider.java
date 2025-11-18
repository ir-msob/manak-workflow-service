package ir.msob.manak.workflow.workflow;

import ir.msob.manak.core.test.jima.crud.base.childdomain.characteristic.BaseCharacteristicCrudDataProvider;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import org.springframework.stereotype.Component;

@Component
public class WorkflowCharacteristicCrudDataProvider extends BaseCharacteristicCrudDataProvider<WorkflowDto, WorkflowService> {
    public WorkflowCharacteristicCrudDataProvider(WorkflowService childService) {
        super(childService);
    }
}
