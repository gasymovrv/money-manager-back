package ru.rgasymov.moneymanager.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rgasymov.moneymanager.domain.dto.request.AccountRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Account;

@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

  AccountResponseDto toDto(Account entity);

  Account fromDto(AccountRequestDto dto);

  List<AccountResponseDto> toDtos(List<Account> entities);
}
