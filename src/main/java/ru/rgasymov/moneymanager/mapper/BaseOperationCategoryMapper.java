package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.entity.BaseOperationCategory;

public interface BaseOperationCategoryMapper<T extends BaseOperationCategory> {

  OperationCategoryResponseDto toDto(T entity);

  List<OperationCategoryResponseDto> toDtos(List<T> entities);
}
