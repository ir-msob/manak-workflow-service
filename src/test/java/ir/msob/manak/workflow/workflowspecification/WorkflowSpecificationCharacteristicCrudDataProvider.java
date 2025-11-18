package ir.msob.manak.workflow.workflowspecification;

import ir.msob.manak.core.test.jima.crud.base.childdomain.characteristic.BaseCharacteristicCrudDataProvider;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import org.springframework.stereotype.Component;

@Component
public class WorkflowSpecificationCharacteristicCrudDataProvider extends BaseCharacteristicCrudDataProvider<WorkflowSpecificationDto, WorkflowSpecificationService> {
    public WorkflowSpecificationCharacteristicCrudDataProvider(WorkflowSpecificationService childService) {
        super(childService);
    }
}
