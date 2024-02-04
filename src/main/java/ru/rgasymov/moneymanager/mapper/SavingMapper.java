package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Saving;

@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SavingMapperDecorator.class)
public interface SavingMapper {

  SavingResponseDto toDto(Saving entity);

  List<SavingResponseDto> toDtos(List<Saving> entities);
}
