package ir.msob.manak.workflow.worker.ai;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.workflow.camunda.CamundaService;
import ir.msob.manak.workflow.worker.common.ActionWorker;
import ir.msob.manak.workflow.workflow.WorkflowService;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
public class AiActionWorker extends ActionWorker {
    private static final Logger logger = LoggerFactory.getLogger(AiActionWorker.class);
    @Getter
    private final AiActionRegistry actionRegistry;

    public AiActionWorker(CamundaService camundaService, WorkflowService workflowService, AiActionRegistry actionRegistry) {
        super(camundaService, workflowService);
        this.actionRegistry = actionRegistry;
    }

    @Transactional
    @Override
    @JobWorker(type = "ai-execution", autoComplete = false)
    public void execute(final ActivatedJob job) {
         super.execute(job);
    }
}
