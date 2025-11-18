package ir.msob.manak.workflow.workflowspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationCriteria;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class WorkflowSpecificationService extends DomainCrudService<WorkflowSpecification, WorkflowSpecificationDto, WorkflowSpecificationCriteria, WorkflowSpecificationRepository>
        implements ChildDomainCrudService<WorkflowSpecificationDto> {

    private final ModelMapper modelMapper;
    private final IdService idService;

    protected WorkflowSpecificationService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, WorkflowSpecificationRepository repository, ModelMapper modelMapper, IdService idService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
    }

    @Override
    public WorkflowSpecificationDto toDto(WorkflowSpecification domain, User user) {
        return modelMapper.map(domain, WorkflowSpecificationDto.class);
    }

    @Override
    public WorkflowSpecification toDomain(WorkflowSpecificationDto dto, User user) {
        return dto;
    }

    @Override
    public Collection<BaseBeforeAfterDomainOperation<String, User, WorkflowSpecificationDto, WorkflowSpecificationCriteria>> getBeforeAfterDomainOperations() {
        return Collections.emptyList();
    }

    @Transactional
    @Override
    public Mono<WorkflowSpecificationDto> getDto(String id, User user) {
        return super.getOne(id, user);
    }

    @Transactional
    @Override
    public Mono<WorkflowSpecificationDto> updateDto(String id, @Valid WorkflowSpecificationDto dto, User user) {
        return super.update(id, dto, user);
    }

    @Override
    public BaseIdService getIdService() {
        return idService;
    }
}
