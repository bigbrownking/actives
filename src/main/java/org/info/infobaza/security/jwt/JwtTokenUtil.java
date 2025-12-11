package org.info.infobaza.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.repository.main.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenUtil {
    private final UserRepository userMainRepository;

    @Value("${jwt.public.key}")
    private String publicKeyPEM;

    @Value("${dossier.jwtSecret}")
    private String jwtSecret;

    @Value("${dossier.admin}")
    private String ADMIN;


    @Value("${dossier.jwtExpirationMs}")
    private int jwtExpirationMs;

    private static final Map<String, Object> header = new HashMap<>();
    static {
        header.put("typ", "JWT");
    }

    // for sso
    private PublicKey getPublicKey() {
        return PublicKeyLoader.loadPublicKey(publicKeyPEM);
    }

    public String getUserNameFromJwtToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("JWT token is null or empty");
            return null;
        }

        // 1️⃣ Try RS256 first
        try {
            String username = Jwts.parser()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            log.debug("Successfully parsed token as SSO (RS256) token");
            return username;
        } catch (Exception ignored) {
        }

        // 2️⃣ Try HS256 next
        try {
            String username = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            log.debug("Successfully parsed token as HS256 token");
            return username;
        } catch (Exception hsEx) {
            log.error("Failed to parse token as both RS256 and HS256. HS256 error: {}", hsEx.getMessage());
            return null;
        }
    }



    public boolean validateToken(String token) {
        try {
            // Try RSA first
            Jwts.parser().setSigningKey(getPublicKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception rsaEx) {
            // Then try HS256
            try {
                Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
                return true;
            } catch (Exception hsEx) {
                log.error("Invalid JWT: {}", hsEx.getMessage());
                return false;
            }
        }
    }


    public String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals(cookieName)) {
                    return parts[1];
                }
            }
        }
        return null;
    }


    // for dossier
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        if (keyBytes.length < 20) {
            throw new IllegalArgumentException("JWT secret key must be at least 64 bytes for HS256 algorithm");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

//    public String generateTokenFromUsername(String username) {
//        Optional<org.info.infobaza.model.main.User> user = userMainRepository.findByUsername(username);
//        if (user.isEmpty()) {
//            log.error("User not found for username: {}", username);
//            throw new IllegalArgumentException("User not found: " + username);
//        }
//
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
//
//        return Jwts.builder()
//                .setHeader(header)
//                .setSubject(username)
//                .claim("user_id", user.get().getId())
//                .claim("token_type", "access")
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }

}
