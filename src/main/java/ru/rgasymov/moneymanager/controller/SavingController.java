package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.request.SavingCriteriaDto;
import ru.rgasymov.moneymanager.domain.dto.response.SavingResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.SearchResultDto;
import ru.rgasymov.moneymanager.service.SavingService;
import ru.rgasymov.moneymanager.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/savings")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class SavingController {

  private final UserService userService;

  private final SavingService savingService;

  @GetMapping
  public SearchResultDto<SavingResponseDto> search(@Valid SavingCriteriaDto criteria) {
    log.info("# Search for savings, criteria: {}, current user: {}", criteria,
        userService.getCurrentUser());
    return savingService.search(criteria);
  }
}
