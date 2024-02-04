package ru.rgasymov.moneymanager.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavingSearchResultDto extends SearchResultDto<SavingResponseDto> {

  private List<OperationCategoryResponseDto> incomeCategories = new ArrayList<>();

  private List<OperationCategoryResponseDto> expenseCategories = new ArrayList<>();
}
