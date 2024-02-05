package ru.rgasymov.moneymanager.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Creates and parses a token for the given authentication.
 * By default, google client-secret is used to sign tokens.
 */
@Slf4j
@Service
public class TokenProvider {

  public TokenProvider(
      @Value("${security.token-secret}") String tokenSecret,
      @Value("${security.token-expiration-period}") Duration tokenExpirationPeriod
  ) {
    this.tokenExpirationPeriod = tokenExpirationPeriod;
    encoder = new NimbusJwtEncoder(
        new ImmutableSecret<>(tokenSecret.getBytes(StandardCharsets.UTF_8)));
    decoder = NimbusJwtDecoder.withSecretKey(
        new SecretKeySpec(
            tokenSecret.getBytes(StandardCharsets.UTF_8),
            MacAlgorithm.HS256.name()
        )
    ).build();
  }

  private final Duration tokenExpirationPeriod;
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;

  public String createToken(Authentication authentication) {
    var userPrincipal = (UserPrincipal) authentication.getPrincipal();

    var now = Instant.now();
    var expiryDate = now.plus(tokenExpirationPeriod);

    var claims = JwtClaimsSet.builder()
        .claim(JwtClaimNames.SUB, userPrincipal.getBusinessUser().getId())
        .claim(JwtClaimNames.IAT, Instant.now())
        .claim(JwtClaimNames.EXP, expiryDate);

    return encoder.encode(
        JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims.build()
        )
    ).getTokenValue();
  }

  public String getUserIdFromToken(Jwt token) {
    return token.getClaim(JwtClaimNames.SUB);
  }

  public Jwt parseToken(String token) {
    try {
      return decoder.decode(token);
    } catch (JwtException ex) {
      log.error("Invalid JWT token", ex);
      throw ex;
    }
  }
}
