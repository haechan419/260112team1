package com.Team1_Back.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

@Slf4j
public class JWTUtil {

    private static String key = "1234567890123456789012345678901234567890";

    public static String generateToken(Map<String, Object> valueMap, int min) {

        SecretKey secretKey = null;

        try {
            secretKey = Keys.hmacShaKeyFor(JWTUtil.key.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        String jwtStr = Jwts.builder()
                .setHeader(Map.of("typ", "JWT"))
                .setClaims(valueMap)
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                .setExpiration(Date.from(ZonedDateTime.now().plusMinutes(min).toInstant()))
                .signWith(secretKey)
                .compact();

        return jwtStr;
    }

    public static Map<String, Object> validateToken(String token) {

        Map<String, Object> claim = null;

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(JWTUtil.key.getBytes("UTF-8"));

            claim = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (MalformedJwtException malformedJwtException) {
            throw new CustomJWTException("MalFormed");
        } catch (ExpiredJwtException expiredJwtException) {
            throw new CustomJWTException("Expired");
        } catch (InvalidClaimException invalidClaimException) {
            throw new CustomJWTException("Invalid");
        } catch (JwtException jwtException) {
            throw new CustomJWTException("JWTError");
        } catch (Exception e) {
            throw new CustomJWTException("Error");
        }

        return claim;
    }
}
