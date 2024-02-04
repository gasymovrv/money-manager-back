package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  public List<HistoryActionDto> findAll() {
    log.info("# Find all operations history, current user: {}", userService.getCurrentUser());
    return historyService.findAll();
  }
}
