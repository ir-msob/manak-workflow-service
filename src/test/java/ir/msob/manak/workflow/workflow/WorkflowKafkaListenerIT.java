package ir.msob.manak.workflow.workflow;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.kafka.domain.DomainCrudKafkaListenerTest;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowCriteria;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowTypeReference;
import ir.msob.manak.workflow.Application;
import ir.msob.manak.workflow.ContainerConfiguration;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {Application.class, ContainerConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@CommonsLog
class WorkflowKafkaListenerIT
        extends DomainCrudKafkaListenerTest<Workflow, WorkflowDto, WorkflowCriteria, WorkflowRepository, WorkflowService, WorkflowDataProvider>
        implements WorkflowTypeReference {

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    void beforeEach() {
        getDataProvider().cleanups();
        WorkflowDataProvider.createMandatoryNewDto();
        WorkflowDataProvider.createNewDto();
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return WorkflowKafkaListener.class;
    }

    @Override
    public String getBaseUri() {
        return WorkflowKafkaListener.BASE_URI;
    }
}
