package ir.msob.manak.workflow.stage;

import ir.msob.manak.core.test.jima.crud.base.childdomain.characteristic.BaseCharacteristicCrudDataProvider;
import ir.msob.manak.domain.model.workflow.stage.StageDto;
import org.springframework.stereotype.Component;

@Component
public class StageCharacteristicCrudDataProvider extends BaseCharacteristicCrudDataProvider<StageDto, StageService> {
    public StageCharacteristicCrudDataProvider(StageService childService) {
        super(childService);
    }
}
