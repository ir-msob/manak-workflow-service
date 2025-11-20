package ir.msob.manak.workflow.workflowspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.manak.core.test.jima.crud.base.domain.DomainCrudDataProvider;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecification;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationCriteria;
import ir.msob.manak.domain.model.workflow.workflowspecification.WorkflowSpecificationDto;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ir.msob.jima.core.test.CoreTestData.DEFAULT_STRING;
import static ir.msob.jima.core.test.CoreTestData.UPDATED_STRING;

/**
 * This class provides test data for the {@link WorkflowSpecification} class. It extends the {@link DomainCrudDataProvider} class
 * and provides methods to create new test data objects, update existing data objects, and generate JSON patches for updates.
 */
@Component
public class WorkflowSpecificationDataProvider extends DomainCrudDataProvider<WorkflowSpecification, WorkflowSpecificationDto, WorkflowSpecificationCriteria, WorkflowSpecificationRepository, WorkflowSpecificationService> {

    private static WorkflowSpecificationDto newDto;
    private static WorkflowSpecificationDto newMandatoryDto;

    protected WorkflowSpecificationDataProvider(BaseIdService idService, ObjectMapper objectMapper, WorkflowSpecificationService service) {
        super(idService, objectMapper, service);
    }

    /**
     * Creates a new DTO object with default values.
     */
    public static void createNewDto() {
        newDto = prepareMandatoryDto();
        newDto.setDescription(DEFAULT_STRING);
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
    public static WorkflowSpecificationDto prepareMandatoryDto() {
        WorkflowSpecificationDto dto = new WorkflowSpecificationDto();
        dto.setName(DEFAULT_STRING);
        return dto;
    }

    /**
     * @throws JsonPointerException if there is an error creating the JSON patch.
     */
    @Override
    @SneakyThrows
    public JsonPatch getJsonPatch() {
        List<JsonPatchOperation> operations = getMandatoryJsonPatchOperation();
        operations.add(new ReplaceOperation(new JsonPointer(String.format("/%s", WorkflowSpecification.FN.description)), new TextNode(UPDATED_STRING)));
        return new JsonPatch(operations);
    }

    /**
     * @throws JsonPointerException if there is an error creating the JSON patch.
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
    public WorkflowSpecificationDto getNewDto() {
        return newDto;
    }

    /**
     * Updates the given DTO object with the updated value for the domain field.
     *
     * @param dto the DTO object to update
     */
    @Override
    public void updateDto(WorkflowSpecificationDto dto) {
        updateMandatoryDto(dto);
        dto.setDescription(UPDATED_STRING);
    }

    /**
     *
     */
    @Override
    public WorkflowSpecificationDto getMandatoryNewDto() {
        return newMandatoryDto;
    }

    /**
     * Updates the given DTO object with the updated value for the mandatory field.
     *
     * @param dto the DTO object to update
     */
    @Override
    public void updateMandatoryDto(WorkflowSpecificationDto dto) {
        dto.setName(UPDATED_STRING);
    }

    /**
     * Creates a list of JSON patch operations for updating the mandatory field.
     *
     * @return a list of JSON patch operations
     * @throws JsonPointerException if there is an error creating the JSON pointer.
     */
    public List<JsonPatchOperation> getMandatoryJsonPatchOperation() throws JsonPointerException {
        List<JsonPatchOperation> operations = new ArrayList<>();
        operations.add(new ReplaceOperation(new JsonPointer(String.format("/%s", WorkflowSpecification.FN.name)), new TextNode(UPDATED_STRING)));
        return operations;
    }

    @Override
    public void assertMandatoryGet(WorkflowSpecificationDto before, WorkflowSpecificationDto after) {
        super.assertMandatoryGet(before, after);
        Assertions.assertThat(after.getName()).isEqualTo(before.getName());
    }

    @Override
    public void assertGet(WorkflowSpecificationDto before, WorkflowSpecificationDto after) {
        super.assertGet(before, after);
        assertMandatoryGet(before, after);

        Assertions.assertThat(after.getDescription()).isEqualTo(before.getDescription());
    }

    @Override
    public void assertMandatoryUpdate(WorkflowSpecificationDto dto, WorkflowSpecificationDto updatedDto) {
        super.assertMandatoryUpdate(dto, updatedDto);
        Assertions.assertThat(dto.getName()).isEqualTo(DEFAULT_STRING);
        Assertions.assertThat(updatedDto.getName()).isEqualTo(UPDATED_STRING);
    }

    @Override
    public void assertUpdate(WorkflowSpecificationDto dto, WorkflowSpecificationDto updatedDto) {
        super.assertUpdate(dto, updatedDto);
        assertMandatoryUpdate(dto, updatedDto);
    }

    @Override
    public void assertMandatorySave(WorkflowSpecificationDto dto, WorkflowSpecificationDto savedDto) {
        super.assertMandatorySave(dto, savedDto);
        assertMandatoryGet(dto, savedDto);
    }

    @Override
    public void assertSave(WorkflowSpecificationDto dto, WorkflowSpecificationDto savedDto) {
        super.assertSave(dto, savedDto);
        assertGet(dto, savedDto);
    }
}