package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.security.JwtResponse;
import org.info.infobaza.model.dossier.User;
import org.info.infobaza.repository.dossier.UserDossierRepository;
import org.info.infobaza.security.jwt.JwtTokenUtil;
import org.info.infobaza.security.UserDetailsImpl;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth/")
public class AuthController {
    private final JwtTokenUtil jwtTokenUtil;
    private final RestTemplate restTemplate;
    private final UserDetailsService userDetailsService;

    // SER SSO
    @GetMapping("fetchTokenCookieAndGetAccess")
    public ResponseEntity<?> fetchTokenCookieAndGetAccess(
            HttpServletRequest request) {
        try {
            String refreshToken = jwtTokenUtil.getTokenFromCookie(request, "refresh_token");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Refresh token not found in cookies");
            }
            String refreshUrl = "https://ser.afm.gov.kz/kfm_new/api/v1/auth/refresh-access-token ";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.add(HttpHeaders.COOKIE, "refresh_token="+refreshToken);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(null, headers);

            ResponseEntity<Map> response = restTemplate.exchange(refreshUrl, HttpMethod.POST, entity, Map.class);

            log.error("RESPONSE: "+ response.getStatusCode());
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                log.info("RB: " + responseBody);
                String newAccessToken = (String) responseBody.get("newAccessToken");

                if (newAccessToken != null) {
                    String username = jwtTokenUtil.getUserNameFromJwtToken(newAccessToken);
                    log.error("USERNAME:  "+ username);
                    UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

                    Authentication authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.error("AUTH: " + authentication.getPrincipal());
                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());

                    return ResponseEntity.ok(new JwtResponse(newAccessToken,
                            userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            roles));
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("New access token not found in response");
                }
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body("Failed to refresh token: " + response.getBody());
            }

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Username not found: " + e.getMessage());
        }
    }
}
