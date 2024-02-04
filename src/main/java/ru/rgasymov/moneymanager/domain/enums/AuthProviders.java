package ru.rgasymov.moneymanager.domain.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProviders {
  GOOGLE("google"),
  VK("vk");

  private final String id;

  public static AuthProviders of(String id) {
    return Arrays.stream(values())
        .filter(v -> v.id.equals(id))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException(
                String.format("Not found auth provider with id '%s'", id)));
  }
}
