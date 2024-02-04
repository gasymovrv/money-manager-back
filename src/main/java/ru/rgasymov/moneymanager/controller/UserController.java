package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.response.UserResponseDto;
import ru.rgasymov.moneymanager.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  @Operation(summary = "Get current user")
  @GetMapping("/current")
  public UserResponseDto current() {
    return userService.getCurrentUserAsDto();
  }
}
