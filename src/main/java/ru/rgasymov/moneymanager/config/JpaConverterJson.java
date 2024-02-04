package ru.rgasymov.moneymanager.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import ru.rgasymov.moneymanager.domain.dto.response.OperationResponseDto;

@Slf4j
@Converter(autoApply = true)
public class JpaConverterJson implements AttributeConverter<OperationResponseDto, String> {

  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
  }

  @Override
  public String convertToDatabaseColumn(OperationResponseDto meta) {
    if (meta == null) {
      return null;
    }

    try {
      return objectMapper.writeValueAsString(meta);
    } catch (JsonProcessingException ex) {
      log.error(
          "# JpaConverterJson: error has occurred while "
              + "writing to DB the action of operations history with id: "
              + meta.getId());
      return null;
    }
  }

  @Override
  public OperationResponseDto convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(dbData, OperationResponseDto.class);
    } catch (IOException ex) {
      log.error(
          "# JpaConverterJson: error has occurred while "
              + "reading from DB the action of operations history: "
              + dbData);
      return null;
    }
  }
}
