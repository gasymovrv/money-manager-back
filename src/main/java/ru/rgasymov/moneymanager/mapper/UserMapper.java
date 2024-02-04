package ru.rgasymov.moneymanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.response.UserResponseDto;
import ru.rgasymov.moneymanager.domain.entity.User;

@Mapper(componentModel = "spring",
    uses = {AccountMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  UserResponseDto toDto(User entity);
}
