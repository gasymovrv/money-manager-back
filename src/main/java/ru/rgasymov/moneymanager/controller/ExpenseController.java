package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
import ru.rgasymov.moneymanager.domain.entity.Expense;
import ru.rgasymov.moneymanager.service.OperationService;
import ru.rgasymov.moneymanager.service.UserService;
import ru.rgasymov.moneymanager.service.expense.ExpenseCategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/expenses")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ExpenseController {

  private final UserService userService;

  private final OperationService<Expense> expenseService;

  private final ExpenseCategoryService expenseCategoryService;

  @PostMapping
  public OperationResponseDto create(@RequestBody @Valid OperationRequestDto dto) {
    log.info("# Create a new expense by dto: {}, current user: {}", dto,
        userService.getCurrentUser());
    return expenseService.create(dto);
  }

  @PutMapping("/{id}")
  public OperationResponseDto update(@PathVariable Long id,
                                     @RequestBody @Valid OperationRequestDto dto) {
    log.info("# Update the expense by id: {}, dto: {}, current user: {}", id, dto,
        userService.getCurrentUser());
    return expenseService.update(id, dto);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    log.info("# Delete an expense by id: {}, current user: {}", id, userService.getCurrentUser());
    expenseService.delete(id);
  }

  @GetMapping("/categories")
  public List<OperationCategoryResponseDto> findAllCategories() {
    final var currentUser = userService.getCurrentUser();
    log.info("# Find all expense categories, current user: {}", currentUser);
    return expenseCategoryService.findAll(currentUser.getCurrentAccount().getId());
  }

  @PostMapping("/categories")
  public OperationCategoryResponseDto createCategory(
      @RequestBody @Valid OperationCategoryRequestDto dto) {
    log.info("# Create a new expense category by dto: {}, current user: {}", dto,
        userService.getCurrentUser());
    return expenseCategoryService.create(dto);
  }

  @PutMapping("/categories/{id}")
  public OperationCategoryResponseDto updateCategory(@PathVariable Long id,
                                                     @RequestBody
                                                     @Valid OperationCategoryRequestDto dto) {
    log.info("# Update the expense category by id: {}, dto: {}, current user: {}", id, dto,
        userService.getCurrentUser());
    return expenseCategoryService.update(id, dto);
  }

  @DeleteMapping(value = "/categories/{id}")
  public void deleteCategory(@PathVariable Long id) {
    log.info("# Delete an expense category by id: {}, current user: {}", id,
        userService.getCurrentUser());
    expenseCategoryService.delete(id);
  }
}
