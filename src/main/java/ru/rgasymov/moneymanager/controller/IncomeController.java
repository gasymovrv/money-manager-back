package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rgasymov.moneymanager.domain.dto.request.OperationCategoryRequestDto;
import ru.rgasymov.moneymanager.domain.dto.request.OperationRequestDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationCategoryResponseDto;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;
import ru.rgasymov.moneymanager.domain.entity.Income;
import ru.rgasymov.moneymanager.service.OperationService;
import ru.rgasymov.moneymanager.service.UserService;
import ru.rgasymov.moneymanager.service.income.IncomeCategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/incomes")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class IncomeController {

  private final UserService userService;

  private final OperationService<Income> incomeService;

  private final IncomeCategoryService incomeCategoryService;

  @PostMapping
  public OperationResponseDto create(@RequestBody @Valid OperationRequestDto dto) {
    log.info("# Create a new income by dto: {}, current user: {}", dto,
        userService.getCurrentUser());
    return incomeService.create(dto);
  }

  @PutMapping("/{id}")
  public OperationResponseDto update(@PathVariable Long id,
                                     @RequestBody @Valid OperationRequestDto dto) {
    log.info("# Update the income by id: {}, dto: {}, current user: {}", id, dto,
        userService.getCurrentUser());
    return incomeService.update(id, dto);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    log.info("# Delete an income by id: {}, current user: {}", id, userService.getCurrentUser());
    incomeService.delete(id);
  }

  @GetMapping("/categories")
  public List<OperationCategoryResponseDto> findAllCategories() {
    final var currentUser = userService.getCurrentUser();
    log.info("# Find all income categories, current user: {}", currentUser);
    return incomeCategoryService.findAll(currentUser.getCurrentAccount().getId());
  }

  @PostMapping("/categories")
  public OperationCategoryResponseDto createCategory(
      @RequestBody @Valid OperationCategoryRequestDto dto) {
    log.info("# Create a new income category by dto: {}, current user: {}", dto,
        userService.getCurrentUser());
    return incomeCategoryService.create(dto);
  }

  @PutMapping("/categories/{id}")
  public OperationCategoryResponseDto updateCategory(@PathVariable Long id,
                                                     @RequestBody
                                                     @Valid OperationCategoryRequestDto dto) {
    log.info("# Update the income category by id: {}, dto: {}, current user: {}", id, dto,
        userService.getCurrentUser());
    return incomeCategoryService.update(id, dto);
  }

  @DeleteMapping("/categories/{id}")
  public void deleteCategory(@PathVariable @NotNull Long id) {
    log.info("# Delete an income category by id: {}, current user: {}", id,
        userService.getCurrentUser());
    incomeCategoryService.delete(id);
  }
}
