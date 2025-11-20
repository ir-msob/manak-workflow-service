package ir.msob.manak.workflow.camunda;

import io.camunda.client.CamundaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * CamundaClient configuration class.
 * Reads gRPC and REST addresses from application.yml
 * and initializes a CamundaClient bean.
 */
@Configuration
public class CamundaClientConfig {

    @Value("${camunda.client.grpc-address}")
    private String grpcAddress;

    @Value("${camunda.client.rest-address}")
    private String restAddress;

    /**
     * Creates and configures a CamundaClient bean using the provided
     * gRPC and REST endpoint addresses.
     *
     * @return a fully initialized CamundaClient instance
     */
    @Bean
    public CamundaClient camundaClient() {
        return CamundaClient.newClientBuilder()
                .grpcAddress(URI.create(grpcAddress))   // Configure gRPC connection
                .restAddress(URI.create(restAddress))   // Configure REST API connection
                .build();
    }
}
