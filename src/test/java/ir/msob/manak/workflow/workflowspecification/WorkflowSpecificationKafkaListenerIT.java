package ir.msob.manak.workflow.workflowspecification;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.kafka.domain.DomainCrudKafkaListenerTest;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationCriteria;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationTypeReference;
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
class WorkflowSpecificationKafkaListenerIT
        extends DomainCrudKafkaListenerTest<WorkflowSpecification, WorkflowSpecificationDto, WorkflowSpecificationCriteria, WorkflowSpecificationRepository, WorkflowSpecificationService, WorkflowSpecificationDataProvider>
        implements WorkflowSpecificationTypeReference {

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    void beforeEach() {
        getDataProvider().cleanups();
        WorkflowSpecificationDataProvider.createMandatoryNewDto();
        WorkflowSpecificationDataProvider.createNewDto();
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return WorkflowSpecificationKafkaListener.class;
    }

    @Override
    public String getBaseUri() {
        return WorkflowSpecificationKafkaListener.BASE_URI;
    }
}
