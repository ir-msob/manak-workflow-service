package ir.msob.manak.workflow.worker.ai;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.action.ActionWorker;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
public class AiActionWorker extends ActionWorker {
    private static final Logger logger = LoggerFactory.getLogger(AiActionWorker.class);
    @Getter
    private final AiActionRegistry actionRegistry;

    public AiActionWorker(CamundaService camundaService, AiActionRegistry actionRegistry) {
        super(camundaService);
        this.actionRegistry = actionRegistry;
    }

    @Transactional
    @Override
    @JobWorker(type = "ai-execution", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        return super.execute(job);
    }
}
