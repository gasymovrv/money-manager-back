package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.IncomeCategory;

@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IncomeCategoryMapper extends BaseOperationCategoryMapper<IncomeCategory> {

  OperationCategoryResponseDto toDto(IncomeCategory entity);

  List<OperationCategoryResponseDto> toDtos(List<IncomeCategory> entities);
}
