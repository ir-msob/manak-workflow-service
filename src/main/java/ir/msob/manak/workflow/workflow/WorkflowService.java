package ir.msob.manak.workflow.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowCriteria;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.workflow.worker.util.WorkflowUtil;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkflowService extends DomainCrudService<Workflow, WorkflowDto, WorkflowCriteria, WorkflowRepository>
        implements ChildDomainCrudService<WorkflowDto> {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final ModelMapper modelMapper;
    private final IdService idService;
    private final UserService userService;

    protected WorkflowService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, WorkflowRepository repository, ModelMapper modelMapper, IdService idService, UserService userService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
        this.userService = userService;
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


    /**
     * Record worker history for the given workflow id.
     * If workflowId is null or empty, this method completes without doing anything.
     */
    @Transactional
    public Mono<Void> recordWorkerHistory(String workflowId, WorkerExecutionStatus status, String error) {
        if (workflowId == null || workflowId.isBlank()) {
            logger.warn("No workflow id available to record worker history. skipping history write.");
            return Mono.empty();
        }

        return this.getOne(workflowId, userService.getSystemUser())
                .flatMap(workflowDto -> {
                    workflowDto.getWorkersHistory().add(prepareWorkerHistory(status, error));
                    return this.update(workflowDto, userService.getSystemUser()).then();
                })
                .doOnSuccess(v -> logger.info("Worker history updated for workflowId={} status={}", workflowId, status))
                .doOnError(ex -> logger.error("Failed to update worker history for workflowId={} error={}", workflowId, ex.getMessage(), ex))
                .onErrorResume(e -> Mono.empty()); // don't fail main flow just because history write failed
    }

    private Workflow.WorkerHistory prepareWorkerHistory(WorkerExecutionStatus workerExecutionStatus, String error) {
        return Workflow.WorkerHistory.builder()
                .executionStatus(workerExecutionStatus)
                .error(error)
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public Mono<WorkflowDto> updateCycleContext(String workflowId,
                                                String cycleId,
                                                Map<String, Object> params) {

        return this.getOne(workflowId, userService.getSystemUser())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Workflow not found. Id=" + workflowId)))
                .flatMap(workflowDto -> {

                    Workflow.Cycle cycle = WorkflowUtil.findCycle(workflowDto, cycleId);
                    if (cycle == null) {
                        return Mono.error(new IllegalStateException(
                                "Cycle not found. workflowId=" + workflowId + ", cycleId=" + cycleId));
                    }

                    // Ensure context is initialized
                    Map<String, Object> context = cycle.getContext();
                    if (context == null) {
                        context = new HashMap<>();
                        cycle.setContext(context);
                    }

                    if (params != null && !params.isEmpty()) {
                        context.putAll(params);
                    }

                    return this.update(workflowDto, userService.getSystemUser());
                })
                .doOnError(e ->
                        logger.warn("Failed to update cycle context: workflowId={}, cycleId={}, error={}",
                                workflowId, cycleId, e.getMessage())
                );
    }

}