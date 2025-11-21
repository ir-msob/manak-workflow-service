package ir.msob.manak.workflow.stage;

import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.manak.core.service.jima.crud.restful.childdomain.service.CharacteristicCrudRestResource;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.stage.Stage;
import ir.msob.manak.domain.model.workflow.stage.StageDto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ir.msob.jima.core.commons.operation.Operations.*;

@RestController
@RequestMapping(StageRestResource.BASE_URI)
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID, EDIT_BY_ID, GET_BY_ID, GET_PAGE})
@Resource(value = Stage.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.RESTFUL)
public class StageCharacteristicRestResource extends CharacteristicCrudRestResource<StageDto, StageService> {

    public StageCharacteristicRestResource(StageService childService, UserService userService) {
        super(childService, userService);
    }
}
