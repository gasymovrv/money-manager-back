package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Income;

@Mapper(componentModel = "spring",
    uses = {IncomeCategoryMapper.class, SavingMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IncomeMapper extends BaseOperationMapper<Income> {

  OperationResponseDto toDto(Income entity);

  List<OperationResponseDto> toDtos(List<Income> entities);
}
