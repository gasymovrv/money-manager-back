package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.response.HistoryActionDto;
import ru.rgasymov.moneymanager.domain.entity.HistoryAction;

@Mapper(componentModel = "spring",
    uses = {AccountMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HistoryMapper {

  HistoryActionDto toDto(HistoryAction entity);

  List<HistoryActionDto> toDtos(List<HistoryAction> entities);
}
