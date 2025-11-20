package ir.msob.manak.workflow;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import ir.msob.jima.core.beans.properties.JimaProperties;
import ir.msob.jima.core.ral.kafka.test.KafkaContainerConfiguration;
import ir.msob.jima.core.ral.mongo.test.configuration.MongoContainerConfiguration;
import ir.msob.jima.security.ral.keycloak.test.KeycloakContainerConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.kafka.KafkaContainer;

@TestConfiguration
public class ContainerConfiguration {
    @Bean
    public DynamicPropertyRegistrar dynamicPropertyRegistrar(JimaProperties jimaProperties
            , MongoDBContainer mongoDBContainer
            , KeycloakContainer keycloakContainer
            , KafkaContainer kafkaContainer
    ) {
        return registry -> {
            MongoContainerConfiguration.registry(registry, mongoDBContainer);
            KeycloakContainerConfiguration.registry(registry, keycloakContainer, jimaProperties);
            KafkaContainerConfiguration.registry(registry, kafkaContainer);
        };
    }
}
