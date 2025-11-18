package ir.msob.manak.workflow.workflowspecification;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.restful.childdomain.BaseCharacteristicCrudRestResourceTest;
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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoConfigureWebTestClient
@SpringBootTest(classes = {Application.class, ContainerConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@CommonsLog
public class WorkflowSpecificationCharacteristicRestResourceIT
        extends BaseCharacteristicCrudRestResourceTest<WorkflowSpecification, WorkflowSpecificationDto, WorkflowSpecificationCriteria, WorkflowSpecificationRepository, WorkflowSpecificationService, WorkflowSpecificationDataProvider, WorkflowSpecificationService, WorkflowSpecificationCharacteristicCrudDataProvider>
        implements WorkflowSpecificationTypeReference {

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        getDataProvider().cleanups();
        WorkflowSpecificationDataProvider.createMandatoryNewDto();
        WorkflowSpecificationDataProvider.createNewDto();
        WorkflowSpecificationCharacteristicCrudDataProvider.createNewChild();
    }


    @Override
    public String getBaseUri() {
        return WorkflowSpecificationRestResource.BASE_URI;
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return WorkflowSpecificationCharacteristicRestResource.class;
    }
}
