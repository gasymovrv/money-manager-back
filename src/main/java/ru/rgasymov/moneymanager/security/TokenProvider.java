package ru.rgasymov.moneymanager.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.time.Duration;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenProvider {

  @Value("${security.token-secret}")
  private String tokenSecret;

  @Value("${security.token-expiration-period}")
  private Duration tokenExpirationPeriod;

  public String createToken(Authentication authentication) {
    var userPrincipal = (UserPrincipal) authentication.getPrincipal();

    var now = new Date();
    var expiryDate = new Date(now.getTime() + tokenExpirationPeriod.toMillis());

    return Jwts.builder()
        .setSubject((userPrincipal.getBusinessUser().getId()))
        .setIssuedAt(new Date())
        .setExpiration(expiryDate)
        .signWith(SignatureAlgorithm.HS512, tokenSecret)
        .compact();
  }

  public String getUserIdFromToken(String token) {
    var claims = Jwts.parser()
        .setSigningKey(tokenSecret)
        .parseClaimsJws(token)
        .getBody();

    return claims.getSubject();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().setSigningKey(tokenSecret).parseClaimsJws(token);
      return true;
    } catch (SignatureException ex) {
      log.error("Invalid JWT signature");
    } catch (MalformedJwtException ex) {
      log.error("Invalid JWT token");
    } catch (ExpiredJwtException ex) {
      log.error("Expired JWT token");
    } catch (UnsupportedJwtException ex) {
      log.error("Unsupported JWT token");
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string is empty.");
    }
    return false;
  }

}
