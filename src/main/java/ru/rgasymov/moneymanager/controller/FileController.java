package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.rgasymov.moneymanager.service.FileService;
import ru.rgasymov.moneymanager.service.expense.ExpenseCategoryService;
import ru.rgasymov.moneymanager.service.income.IncomeCategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/files")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class FileController {

  private final FileService fileService;

  private final ExpenseCategoryService expenseCategoryService;
  private final IncomeCategoryService incomeCategoryService;

  @RequestMapping(value = "/xlsx/import",
      method = RequestMethod.POST,
      consumes = "multipart/form-data")
  public void importFromXlsx(@RequestPart("file") MultipartFile file) {
    log.info("# Import from xlsx file");
    fileService.importFromXlsx(file);
    expenseCategoryService.clearCachedCategories();
    incomeCategoryService.clearCachedCategories();
    log.info("# Import from the file has successfully completed");
  }

  @GetMapping("/xlsx/export")
  public ResponseEntity<Resource> exportToXlsx() {
    log.info("# Export to xlsx file");
    var result = fileService.exportToXlsx();
    log.info("# Export to the file has successfully completed");
    return result;
  }

  @GetMapping("/xlsx/template")
  public ResponseEntity<Resource> downloadXlsxTemplate() {
    log.info("# Download xlsx template");
    return fileService.getXlsxTemplate();
  }
}
