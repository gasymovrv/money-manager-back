package ru.rgasymov.moneymanager.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

@Slf4j
public final class TokenUtils {

  private TokenUtils() {
  }

  public static long getExpiresIn(Map<String, Object> tokenResponseParameters) {
    return getParameterValue(tokenResponseParameters, OAuth2ParameterNames.EXPIRES_IN, 0L);
  }

  public static Set<String> getScopes(Map<String, Object> tokenResponseParameters) {
    if (tokenResponseParameters.containsKey(OAuth2ParameterNames.SCOPE)) {
      String scope = getParameterValue(tokenResponseParameters, OAuth2ParameterNames.SCOPE);
      return new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
    }
    return Collections.emptySet();
  }

  public static long getParameterValue(Map<String, Object> tokenResponseParameters,
                                        String parameterName,
                                        long defaultValue) {
    var parameterValue = defaultValue;

    var obj = tokenResponseParameters.get(parameterName);
    if (obj != null) {
      // Final classes Long and Integer do not need to be coerced
      if (obj.getClass() == Long.class) {
        parameterValue = (Long) obj;
      } else if (obj.getClass() == Integer.class) {
        parameterValue = (Integer) obj;
      } else {
        // Attempt to coerce to a long (typically from a String)
        try {
          parameterValue = Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
          log.error("Failed to parse token response parameter", e);
        }
      }
    }

    return parameterValue;
  }

  public static String getParameterValue(Map<String, Object> tokenResponseParameters,
                                          String parameterName) {
    Object obj = tokenResponseParameters.get(parameterName);
    return (obj != null) ? obj.toString() : null;
  }
}
