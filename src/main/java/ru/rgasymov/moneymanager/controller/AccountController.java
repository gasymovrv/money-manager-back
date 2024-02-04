package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.request.AccountRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto;
import ru.rgasymov.moneymanager.service.AccountService;
import ru.rgasymov.moneymanager.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/accounts")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AccountController {

  private final UserService userService;

  private final AccountService accountService;

  @GetMapping()
  public List<AccountResponseDto> findAll() {
    log.info("# Find all accounts, current user: {}", userService.getCurrentUser());
    return accountService.findAll();
  }

  @PostMapping()
  public AccountResponseDto create(@RequestBody @Valid AccountRequestDto dto) {
    log.info("# Create a new account by dto: {}, current user: {}", dto,
        userService.getCurrentUser());
    return accountService.create(dto);
  }

  @PutMapping("/{id}")
  public AccountResponseDto update(@PathVariable Long id,
                                   @RequestBody @Valid AccountRequestDto dto) {
    log.info("# Update the account by id: {}, dto: {}, current user: {}", id, dto,
        userService.getCurrentUser());
    return accountService.update(id, dto);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    log.info("# Delete an account by id: {}, current user: {}", id, userService.getCurrentUser());
    accountService.delete(id);
  }

  @PostMapping("/change")
  public AccountResponseDto changeCurrent(@RequestParam Long id) {
    log.info("# Change current account to another with id: {}, current user: {}", id,
        userService.getCurrentUser());
    return accountService.changeCurrent(id);
  }

  @GetMapping("/currencies")
  public List<String> getAllCurrencies() {
    log.info("# Get all currencies, current user: {}", userService.getCurrentUser());
    return Currency.getAvailableCurrencies()
        .stream()
        .map(Currency::getCurrencyCode)
        .sorted()
        .collect(Collectors.toList());
  }

  @PostMapping("/default-categories")
  public void createDefaultCategories() {
    log.info("# Create default categories, current user: {}", userService.getCurrentUser());
    accountService.createDefaultCategories();
  }
}
