package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.BaseOperation;

public interface BaseOperationMapper<T extends BaseOperation> {

  OperationResponseDto toDto(T entity);

  List<OperationResponseDto> toDtos(List<T> entities);
}
