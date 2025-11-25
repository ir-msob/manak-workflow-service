package ir.msob.manak.workflow.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.manak.core.test.jima.crud.base.domain.DomainCrudDataProvider;
import ir.msob.manak.domain.model.workflow.workflow.Workflow;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowCriteria;
import ir.msob.manak.domain.model.workflow.workflow.WorkflowDto;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ir.msob.jima.core.test.CoreTestData.UPDATED_STRING;

/**
 * This class provides test data for the {@link Workflow} class. It extends the {@link DomainCrudDataProvider} class
 * and provides methods to create new test data objects, update existing data objects, and generate JSON patches for updates.
 */
@Component
public class WorkflowDataProvider extends DomainCrudDataProvider<Workflow, WorkflowDto, WorkflowCriteria, WorkflowRepository, WorkflowService> {

    private static WorkflowDto newDto;
    private static WorkflowDto newMandatoryDto;

    protected WorkflowDataProvider(BaseIdService idService, ObjectMapper objectMapper, WorkflowService service) {
        super(idService, objectMapper, service);
    }

    /**
     * Creates a new DTO object with default values.
     */
    public static void createNewDto() {
        newDto = prepareMandatoryDto();
    }

    /**
     * Creates a new DTO object with mandatory fields set.
     */
    public static void createMandatoryNewDto() {
        newMandatoryDto = prepareMandatoryDto();
    }

    /**
     * Creates a new DTO object with mandatory fields set.
     */
    public static WorkflowDto prepareMandatoryDto() {
        return new WorkflowDto();
    }

    /**
     *
     */
    @Override
    @SneakyThrows
    public JsonPatch getJsonPatch() {
        List<JsonPatchOperation> operations = getMandatoryJsonPatchOperation();
        operations.add(new ReplaceOperation(new JsonPointer(String.format("/%s", Workflow.FN.description)), new TextNode(UPDATED_STRING)));
        return new JsonPatch(operations);
    }

    /**
     *
     */
    @Override
    @SneakyThrows
    public JsonPatch getMandatoryJsonPatch() {
        return new JsonPatch(getMandatoryJsonPatchOperation());
    }

    /**
     *
     */
    @Override
    public WorkflowDto getNewDto() {
        return newDto;
    }

    /**
     * Updates the given DTO object with the updated value for the domain field.
     *
     * @param dto the DTO object to update
     */
    @Override
    public void updateDto(WorkflowDto dto) {
        updateMandatoryDto(dto);
    }

    /**
     *
     */
    @Override
    public WorkflowDto getMandatoryNewDto() {
        return newMandatoryDto;
    }

    /**
     * Updates the given DTO object with the updated value for the mandatory field.
     *
     * @param dto the DTO object to update
     */
    @Override
    public void updateMandatoryDto(WorkflowDto dto) {
    }

    /**
     * Creates a list of JSON patch operations for updating the mandatory field.
     *
     * @return a list of JSON patch operations
     * @throws JsonPointerException if there is an error creating the JSON pointer.
     */
    public List<JsonPatchOperation> getMandatoryJsonPatchOperation() throws JsonPointerException {
        List<JsonPatchOperation> operations = new ArrayList<>();
        operations.add(new ReplaceOperation(new JsonPointer(String.format("/%s", Workflow.FN.name)), new TextNode(UPDATED_STRING)));
        return operations;
    }

    @Override
    public void assertMandatoryGet(WorkflowDto before, WorkflowDto after) {
        super.assertMandatoryGet(before, after);
    }

    @Override
    public void assertGet(WorkflowDto before, WorkflowDto after) {
        super.assertGet(before, after);
        assertMandatoryGet(before, after);

    }

    @Override
    public void assertMandatoryUpdate(WorkflowDto dto, WorkflowDto updatedDto) {
        super.assertMandatoryUpdate(dto, updatedDto);
    }

    @Override
    public void assertUpdate(WorkflowDto dto, WorkflowDto updatedDto) {
        super.assertUpdate(dto, updatedDto);
        assertMandatoryUpdate(dto, updatedDto);
    }

    @Override
    public void assertMandatorySave(WorkflowDto dto, WorkflowDto savedDto) {
        super.assertMandatorySave(dto, savedDto);
        assertMandatoryGet(dto, savedDto);
    }

    @Override
    public void assertSave(WorkflowDto dto, WorkflowDto savedDto) {
        super.assertSave(dto, savedDto);
        assertGet(dto, savedDto);
    }
}