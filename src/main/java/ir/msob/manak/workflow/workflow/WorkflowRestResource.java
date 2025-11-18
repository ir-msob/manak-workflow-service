package ir.msob.manak.workflow.workflow;

import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.manak.core.service.jima.crud.restful.domain.service.DomainCrudRestResource;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowCriteria;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ir.msob.jima.core.commons.operation.Operations.*;

@RestController
@RequestMapping(WorkflowRestResource.BASE_URI)
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID, EDIT_BY_ID, GET_BY_ID, GET_PAGE})
@Resource(value = Workflow.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.RESTFUL)
public class WorkflowRestResource extends DomainCrudRestResource<Workflow, WorkflowDto, WorkflowCriteria, WorkflowRepository, WorkflowService> {
    public static final String BASE_URI = "/api/v1/" +Workflow.DOMAIN_NAME_WITH_HYPHEN;

    protected WorkflowRestResource(UserService userService, WorkflowService service) {
        super(userService, service);
    }
}
