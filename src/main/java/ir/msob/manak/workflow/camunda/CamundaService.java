package ir.msob.manak.workflow.camunda;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CamundaService {
    private static final Logger logger = LoggerFactory.getLogger(CamundaService.class);

    private final CamundaClient camundaClient;

    /**
     * Complete Camunda job using CamundaClient's async API.
     * Wrapped into a Mono so it can be composed reactively.
     */
    public Mono<Void> complete(ActivatedJob job, Map<String, Object> resultVars) {
        Objects.requireNonNull(job, "job must not be null");

        return Mono.create(sink -> {
            try {
                camundaClient
                        .newCompleteCommand(job.getKey())
                        .variables(resultVars)
                        .send()
                        .whenComplete((resp, ex) -> {
                            if (ex != null) {
                                logger.error("Failed to complete job in Camunda. jobKey={} error={}", job.getKey(), ex.getMessage(), ex);
                                sink.error(ex);
                            } else {
                                logger.info("Job completed in Camunda successfully. jobKey={}", job.getKey());
                                sink.success();
                            }
                        });
            } catch (Exception ex) {
                logger.error("Exception while sending complete command to Camunda. jobKey={} error={}", job.getKey(), ex.getMessage(), ex);
                sink.error(ex);
            }
        });
    }
}
