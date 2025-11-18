package ir.msob.manak.workflow.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowCriteria;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class WorkflowService extends DomainCrudService<Workflow, WorkflowDto, WorkflowCriteria, WorkflowRepository>
        implements ChildDomainCrudService<WorkflowDto> {

    private final ModelMapper modelMapper;
    private final IdService idService;

    protected WorkflowService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, WorkflowRepository repository, ModelMapper modelMapper, IdService idService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
    }

    @Override
    public WorkflowDto toDto(Workflow domain, User user) {
        return modelMapper.map(domain, WorkflowDto.class);
    }

    @Override
    public Workflow toDomain(WorkflowDto dto, User user) {
        return dto;
    }

    @Override
    public Collection<BaseBeforeAfterDomainOperation<String, User, WorkflowDto, WorkflowCriteria>> getBeforeAfterDomainOperations() {
        return Collections.emptyList();
    }

    @Transactional
    @Override
    public Mono<WorkflowDto> getDto(String id, User user) {
        return super.getOne(id, user);
    }

    @Transactional
    @Override
    public Mono<WorkflowDto> updateDto(String id, @Valid WorkflowDto dto, User user) {
        return super.update(id, dto, user);
    }

    @Override
    public BaseIdService getIdService() {
        return idService;
    }
}
