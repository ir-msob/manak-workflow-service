package ir.msob.manak.workflow.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.stage.Stage;
import ir.msob.manak.domain.model.workflow.stage.StageCriteria;
import ir.msob.manak.domain.model.workflow.stage.StageDto;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class StageService extends DomainCrudService<Stage, StageDto, StageCriteria, StageRepository>
        implements ChildDomainCrudService<StageDto> {

    private final ModelMapper modelMapper;
    private final IdService idService;

    protected StageService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, StageRepository repository, ModelMapper modelMapper, IdService idService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
    }

    @Override
    public StageDto toDto(Stage domain, User user) {
        return modelMapper.map(domain, StageDto.class);
    }

    @Override
    public Stage toDomain(StageDto dto, User user) {
        return dto;
    }

    @Override
    public Collection<BaseBeforeAfterDomainOperation<String, User, StageDto, StageCriteria>> getBeforeAfterDomainOperations() {
        return Collections.emptyList();
    }

    @Transactional
    @Override
    public Mono<StageDto> getDto(String id, User user) {
        return super.getOne(id, user);
    }

    @Transactional
    @Override
    public Mono<StageDto> updateDto(String id, @Valid StageDto dto, User user) {
        return super.update(id, dto, user);
    }

    @Override
    public BaseIdService getIdService() {
        return idService;
    }
}
