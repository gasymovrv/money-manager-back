package ru.rgasymov.moneymanager.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rgasymov.moneymanager.domain.FileExportData;
import ru.rgasymov.moneymanager.domain.dto.request.SavingCriteriaDto;
import ru.rgasymov.moneymanager.exception.EmptyDataGenerationException;
import ru.rgasymov.moneymanager.service.xlsx.XlsxFileService;

@Service
@RequiredArgsConstructor
public class FileService {

  private final XlsxFileService xlsxFileService;
  private final ImportService importService;
  private final SavingService savingService;
  private final UserService userService;

  @Value("${xlsx.max-exported-rows}")
  private int maxExportedRows;

  public void importFromXlsx(MultipartFile file) {
    var parsingResult = xlsxFileService.parse(file);
    importService.importFromFile(parsingResult);
  }

  public ResponseEntity<Resource> exportToXlsx() {
    var criteria = new SavingCriteriaDto();
    criteria.setPageSize(maxExportedRows);
    var account = userService.getCurrentUserAsDto().getCurrentAccount();
    var result = savingService.search(criteria);
    var savings = result.getResult();
    if (CollectionUtils.isEmpty(savings)) {
      throw new EmptyDataGenerationException("There is no data in current account to export");
    }

    return xlsxFileService.generate(
        new FileExportData(
            account,
            savings,
            result.getIncomeCategories(),
            result.getExpenseCategories()));
  }

  public ResponseEntity<Resource> getXlsxTemplate() {
    return xlsxFileService.getTemplate();
  }
}
