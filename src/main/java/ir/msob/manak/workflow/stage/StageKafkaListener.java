package ir.msob.manak.workflow.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.client.BaseAsyncClient;
import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.jima.crud.api.kafka.client.ChannelUtil;
import ir.msob.manak.core.service.jima.crud.kafka.domain.service.DomainCrudKafkaListener;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.workflow.stage.Stage;
import ir.msob.manak.domain.model.workflow.stage.StageCriteria;
import ir.msob.manak.domain.model.workflow.stage.StageDto;
import ir.msob.manak.domain.model.workflow.stage.StageTypeReference;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import static ir.msob.jima.core.commons.operation.Operations.*;

@Component
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID})
@Resource(value = Stage.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.KAFKA)
public class StageKafkaListener
        extends DomainCrudKafkaListener<Stage, StageDto, StageCriteria, StageRepository, StageService>
        implements StageTypeReference {
    public static final String BASE_URI = ChannelUtil.getBaseChannel(StageDto.class);

    protected StageKafkaListener(UserService userService, StageService service, ObjectMapper objectMapper, ConsumerFactory<String, String> consumerFactory, BaseAsyncClient asyncClient) {
        super(userService, service, objectMapper, consumerFactory, asyncClient);
    }

}
