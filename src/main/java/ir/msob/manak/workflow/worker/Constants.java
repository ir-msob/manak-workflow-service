package ir.msob.manak.workflow.worker;

import java.util.regex.Pattern;

public class Constants {
    public static final String ACTION_KEY = "action";
    public static final String PARAMS_KEY = "params";
    public static final String WORKFLOW_SPECIFICATION_ID_KEY = "workflowSpecificationId";
    public static final String WORKFLOW_ID_KEY = "workflowId";
    public static final String STAGE_KEY_KEY = "stageKey";
    public static final String STAGE_TYPE_KEY = "stageType";
    public static final String CYCLE_ID_KEY = "cycleId";
    public static final String STAGE_HISTORY_ID_KEY = "stageHistoryId";
    public static final String STAGE_OUTPUT_KEY = "stageOutput";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String WORKFLOW_EXECUTION_STATUS_KEY = "workflowExecutionStatus";
    public static final String CYCLE_EXECUTION_STATUS_KEY = "cycleExecutionStatus";
    public static final String STAGE_EXECUTION_STATUS_KEY = "stageExecutionStatus";
    public static final String STAGE_EXECUTION_ERROR_KEY = "stageExecutionError";
    public static final String WORKER_EXECUTION_STATUS_KEY = "workerExecutionStatus";
    public static final String WORKER_EXECUTION_ERROR_KEY = "workerExecutionError";
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";
    public static final String CYCLE_CONTEXT_KEY = "cycleContext";
    public static final String PROCESS_VARIABLE_KEY = "processVariable";
    public static final String VARIABLE_START_CHAR = "$";
    public static final String REPOSITORY_BRANCHES_KEY = "repositoryBranches";
    public static final String REPOSITORY_PULL_REQUESTS_KEY = "repositoryPullRequests";
    public static final String REPOSITORY_DIFF_PATCHES_KEY = "repositoryDiffPatches";
    public static final String RESOURCE_CONTENTS_KEY = "resourceContents";
    public static final String RESOURCE_OVERVIEWS_KEY = "resourceOverviews";
    public static final String SOLUTION_KEY = "solution";
    public static final String CONTENT_KEY = "content";
    public static final String OPTIMIZED_CONTENT_KEY = "optimizedContent";
    public static final String AI_TOOLS_KEY = "aiTools";
    public static final String AI_MODEL_KEY = "aiModel";
    public static final String AI_PROMPT_TEMPLATE_KEY = "aiPromptTemplate";
    public static final String AI_RESPONSE_SCHEMA_KEY = "aiResponseSchema";
    public static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private Constants() {
    }
}
