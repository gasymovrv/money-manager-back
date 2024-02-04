package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.ExpenseCategory;

@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseCategoryMapper extends BaseOperationCategoryMapper<ExpenseCategory> {

  OperationCategoryResponseDto toDto(ExpenseCategory entity);

  List<OperationCategoryResponseDto> toDtos(List<ExpenseCategory> entities);
}
