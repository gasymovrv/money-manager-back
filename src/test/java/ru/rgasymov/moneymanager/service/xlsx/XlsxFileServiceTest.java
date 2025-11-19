package ru.rgasymov.moneymanager.service.xlsx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.rgasymov.moneymanager.domain.FileExportData;
import ru.rgasymov.moneymanager.domain.FileImportResult;
import ru.rgasymov.moneymanager.domain.dto.response.AccountResponseDto;
import ru.rgasymov.moneymanager.domain.enums.AccountTheme;
import ru.rgasymov.moneymanager.exception.DataExtractionException;

@ExtendWith(MockitoExtension.class)
class XlsxFileServiceTest {

  @Mock
  private XlsxParsingService xlsxParsingService;

  @Mock
  private XlsxGenerationService xlsxGenerationService;

  @Mock
  private MultipartFile multipartFile;

  private XlsxFileService xlsxFileService;

  @BeforeEach
  void setUp() {
    xlsxFileService = new XlsxFileService(xlsxParsingService, xlsxGenerationService);
    ReflectionTestUtils.setField(xlsxFileService, "root", "uploaded-files");
    ReflectionTestUtils.setField(xlsxFileService, "deleteImportFiles", true);
  }

  @Test
  void parse_shouldParseFile_whenValidFile() throws Exception {
    when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    var importResult = new FileImportResult(List.of(), List.of(), Set.of(), Set.of());
    when(xlsxParsingService.parse(any(File.class))).thenReturn(importResult);

    var result = xlsxFileService.parse(multipartFile);

    assertThat(result).isNotNull();
    verify(xlsxParsingService).parse(any(File.class));
  }

  @Test
  void parse_shouldThrowException_whenParsingFails() throws Exception {
    when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(xlsxParsingService.parse(any(File.class))).thenThrow(new RuntimeException("Parse error"));

    assertThatThrownBy(() -> xlsxFileService.parse(multipartFile))
        .isInstanceOf(DataExtractionException.class);
  }

  @Test
  void generate_shouldGenerateFile() throws Exception {
    var account = new AccountResponseDto();
    account.setId(1L);
    account.setName("Test Account");
    account.setTheme(AccountTheme.LIGHT);
    account.setCurrency("USD");

    var exportData = new FileExportData(account, List.of(), List.of(), List.of());
    var resource = new ByteArrayResource(new byte[0]);

    when(xlsxGenerationService.generate(any(), eq(exportData))).thenReturn(resource);

    var response = xlsxFileService.generate(exportData);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    verify(xlsxGenerationService).generate(any(), eq(exportData));
  }

  @Test
  void getTemplate_shouldReturnTemplate() {
    var response = xlsxFileService.getTemplate();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
  }
}
