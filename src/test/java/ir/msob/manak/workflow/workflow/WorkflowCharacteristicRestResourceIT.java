package ir.msob.manak.workflow.workflow;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.restful.childdomain.BaseCharacteristicCrudRestResourceTest;
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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoConfigureWebTestClient
@SpringBootTest(classes = {Application.class, ContainerConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@CommonsLog
class WorkflowCharacteristicRestResourceIT
        extends BaseCharacteristicCrudRestResourceTest<Workflow, WorkflowDto, WorkflowCriteria, WorkflowRepository, WorkflowService, WorkflowDataProvider, WorkflowService, WorkflowCharacteristicCrudDataProvider>
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
        WorkflowCharacteristicCrudDataProvider.createNewChild();
    }


    @Override
    public String getBaseUri() {
        return WorkflowRestResource.BASE_URI;
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return WorkflowCharacteristicRestResource.class;
    }
}
