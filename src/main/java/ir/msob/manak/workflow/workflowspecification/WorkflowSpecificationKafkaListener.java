package ir.msob.manak.workflow.workflowspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.client.BaseAsyncClient;
import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.jima.crud.api.kafka.client.ChannelUtil;
import ir.msob.manak.core.service.jima.crud.kafka.domain.service.DomainCrudKafkaListener;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationCriteria;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationTypeReference;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import static ir.msob.jima.core.commons.operation.Operations.*;

@Component
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID})
@Resource(value = WorkflowSpecification.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.KAFKA)
public class WorkflowSpecificationKafkaListener
        extends DomainCrudKafkaListener<WorkflowSpecification, WorkflowSpecificationDto, WorkflowSpecificationCriteria, WorkflowSpecificationRepository, WorkflowSpecificationService>
        implements WorkflowSpecificationTypeReference {
    public static final String BASE_URI = ChannelUtil.getBaseChannel(WorkflowSpecificationDto.class);

    protected WorkflowSpecificationKafkaListener(UserService userService, WorkflowSpecificationService service, ObjectMapper objectMapper, ConsumerFactory<String, String> consumerFactory, BaseAsyncClient asyncClient) {
        super(userService, service, objectMapper, consumerFactory, asyncClient);
    }

}
