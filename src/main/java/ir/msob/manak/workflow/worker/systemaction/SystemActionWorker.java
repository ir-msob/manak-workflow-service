package ir.msob.manak.workflow.worker.systemaction;

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
public class SystemActionWorker extends ActionWorker {

    private static final Logger logger = LoggerFactory.getLogger(SystemActionWorker.class);
    @Getter
    private final SystemActionActionRegistry actionRegistry;

    public SystemActionWorker(CamundaService camundaService, SystemActionActionRegistry actionRegistry) {
        super(camundaService);
        this.actionRegistry = actionRegistry;
    }

    @Transactional
    @Override
    @JobWorker(type = "system-action", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        return super.execute(job);
    }
}