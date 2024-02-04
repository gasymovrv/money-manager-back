package ru.rgasymov.moneymanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${server.api-base-url}/version")
public class VersionController {

  private final BuildProperties buildProperties;

  @Operation(summary = "Get current version")
  @GetMapping
  public String version() {
    return buildProperties.getVersion();
  }
}
