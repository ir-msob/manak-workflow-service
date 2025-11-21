package ir.msob.manak.workflow.stage;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.kafka.domain.DomainCrudKafkaListenerTest;
import ir.msob.manak.domain.model.workflow.stage.Stage;
import ir.msob.manak.domain.model.workflow.stage.StageCriteria;
import ir.msob.manak.domain.model.workflow.stage.StageDto;
import ir.msob.manak.domain.model.workflow.stage.StageTypeReference;
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
public class StageKafkaListenerIT
        extends DomainCrudKafkaListenerTest<Stage, StageDto, StageCriteria, StageRepository, StageService, StageDataProvider>
        implements StageTypeReference {

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        getDataProvider().cleanups();
        StageDataProvider.createMandatoryNewDto();
        StageDataProvider.createNewDto();
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return StageKafkaListener.class;
    }

    @Override
    public String getBaseUri() {
        return StageKafkaListener.BASE_URI;
    }
}
