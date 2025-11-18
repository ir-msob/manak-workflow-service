package ir.msob.manak.workflow.workflowspecification;

import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.manak.core.service.jima.crud.restful.childdomain.service.CharacteristicCrudRestResource;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ir.msob.jima.core.commons.operation.Operations.*;

@RestController
@RequestMapping(WorkflowSpecificationRestResource.BASE_URI)
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID, EDIT_BY_ID, GET_BY_ID, GET_PAGE})
@Resource(value = WorkflowSpecification.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.RESTFUL)
public class WorkflowSpecificationCharacteristicRestResource extends CharacteristicCrudRestResource<WorkflowSpecificationDto, WorkflowSpecificationService> {

    public WorkflowSpecificationCharacteristicRestResource(WorkflowSpecificationService childService, UserService userService) {
        super(childService, userService);
    }
}
