package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.response.HistoryActionDto;
import ru.rgasymov.moneymanager.service.HistoryService;
import ru.rgasymov.moneymanager.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/history")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class HistoryController {

  private final UserService userService;

  private final HistoryService historyService;

  @GetMapping()
  public Page<HistoryActionDto> findAll(
      @PageableDefault(sort = {"modifiedAt"}, size = 20) Pageable pageable) {
    log.info("# Find all operations history, current user: {}", userService.getCurrentUser());
    return historyService.findAll(pageable);
  }
}
