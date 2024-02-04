package ru.rgasymov.moneymanager.service.xlsx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rgasymov.moneymanager.constant.DateTimeFormats;
import ru.rgasymov.moneymanager.domain.FileExportData;
import ru.rgasymov.moneymanager.domain.FileImportResult;
import ru.rgasymov.moneymanager.exception.DataExtractionException;
import ru.rgasymov.moneymanager.exception.DataGenerationException;
import ru.rgasymov.moneymanager.exception.FileReadingException;
import ru.rgasymov.moneymanager.exception.IncorrectFileStorageRootException;

@Service
@RequiredArgsConstructor
@Slf4j
public class XlsxFileService {

  private static final String UPLOADED_FILE_NAME_PATTERN = "%s/%s_%s.%s";
  private static final String DOWNLOADED_FILE_NAME_PATTERN = "%s_%s_%s.xlsx";
  private static final String DOWNLOADED_TEMPLATE_FILE_NAME = "money-manager-template.xlsx";
  private static final String PATH_TO_GENERATION_TEMPLATE = "xlsx/generation-template.xlsx";
  private static final String PATH_TO_USER_TEMPLATE = "xlsx/user-template.xlsx";

  private final XlsxParsingService xlsxParsingService;

  private final XlsxGenerationService xlsxGenerationService;

  @Value("${file-service.root}")
  private String root;

  @Value("${file-service.delete-import-files}")
  private Boolean deleteImportFiles;

  public FileImportResult parse(MultipartFile multipartFile) {
    log.info("# XlsxFileService: file parsing has started");
    var rootPath = Paths.get(root);
    createRootIfNotExists(rootPath);

    var originalFileName = multipartFile.getOriginalFilename();
    var destination = Paths.get(generateFilePath(originalFileName, rootPath)).toFile();
    try {
      FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), destination);
      log.info("# File has written on disk: {}, original name: {}", destination.getName(),
          originalFileName);
    } catch (IOException e) {
      throw new FileReadingException(
          String.format("Error while reading content from file '%s'", originalFileName));
    }

    try {
      FileImportResult result = xlsxParsingService.parse(destination);
      if (deleteImportFiles) {
        destination.delete();
      }
      log.info("# XlsxFileService: file parsing has successfully completed");
      return result;

    } catch (Exception e) {
      log.error("# XlsxFileService: error has occurred while parsing the file");
      destination.delete();
      throw new DataExtractionException(e);
    }
  }

  public ResponseEntity<Resource> generate(FileExportData data) {
    log.info("# XlsxFileService: file generation has started");

    try {
      var template = new ClassPathResource(PATH_TO_GENERATION_TEMPLATE);
      var result = xlsxGenerationService.generate(template, data);
      log.info("# XlsxFileService: file generation has successfully completed");

      var fileName = String.format(DOWNLOADED_FILE_NAME_PATTERN,
          data.account().getName().replaceAll("\\s", "_"),
          data.account().getCurrency(),
          LocalDateTime.now()
              .format(DateTimeFormatter
                  .ofPattern(DateTimeFormats.FILE_NAME_DATE_TIME_FORMAT)));

      var contentDisposition = ContentDisposition.builder("attachment")
          .filename(fileName, StandardCharsets.UTF_8)
          .build();
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(result);

    } catch (Exception e) {
      log.error("# XlsxFileService: error has occurred while generating the file");
      throw new DataGenerationException(e);
    }
  }

  public ResponseEntity<Resource> getTemplate() {
    ClassPathResource resource = new ClassPathResource(PATH_TO_USER_TEMPLATE);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment; filename=\"%s\"", DOWNLOADED_TEMPLATE_FILE_NAME))
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }

  private String generateFilePath(String originalFileName, Path rootPath) {
    return String.format(
        UPLOADED_FILE_NAME_PATTERN, rootPath.toString(),
        LocalDateTime.now().format(DateTimeFormatter
            .ofPattern(DateTimeFormats.FILE_NAME_DATE_TIME_FORMAT)),
        UUID.randomUUID(),
        FilenameUtils.getExtension(originalFileName));
  }

  private void createRootIfNotExists(Path rootPath) {
    if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
      try {
        Files.createDirectory(rootPath);
      } catch (IOException e) {
        throw new IncorrectFileStorageRootException(
            String.format("File storage root '%s' is incorrect, could not create directory", root));
      }
    }
  }
}
