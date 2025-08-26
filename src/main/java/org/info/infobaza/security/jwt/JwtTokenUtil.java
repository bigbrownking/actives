package org.info.infobaza.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.dossier.User;
import org.info.infobaza.repository.dossier.UserDossierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenUtil {
    private final UserDossierRepository userRepository;

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
        String username;

        try {
            username = Jwts.parser()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            log.debug("Successfully parsed token as SSO token");
            return username;

        } catch (Exception ssoException) {
            log.error("Failed to parse token as both regular JWT and SSO token. Regular JWT error: {}, SSO error: {}",
                    ssoException.getMessage(), ssoException.getMessage());
            return "";
        }
    }


    public boolean validateSSOToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
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

    public String generateDossierJwtToken() {
        User user = userRepository.findByUsernameTwo(ADMIN);
        if (user == null) {
            log.error("User not found for username: {}", ADMIN);
            throw new IllegalStateException("Dossier admin user not found");
        }
        Integer tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;

        int expiration = jwtExpirationMs;

        return Jwts.builder()
                .setSubject(user.getIin())
                .setHeader(header)
                .claim("user_id", user.getId())
                .claim("tokenVersion", tokenVersion)
                .claim("token_type", "access")
                .claim("jti", "364219c66b1b4e3d9159816490360768")
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
