package org.info.infobaza.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.request.SignUpRequest;
import org.info.infobaza.dto.response.security.JwtResponse;
import org.info.infobaza.model.main.User;
import org.info.infobaza.repository.main.UserRepository;
import org.info.infobaza.security.UserDetailsImpl;
import org.info.infobaza.security.jwt.JwtTokenUtil;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // SER SSO
    @GetMapping("fetchTokenCookieAndGetAccess")
    public ResponseEntity<?> fetchTokenCookieAndGetAccess(
            HttpServletRequest request) {
        try {
            String refreshToken = jwtTokenUtil.getTokenFromCookie(request, "refresh_token");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Refresh token not found in cookies");
            }
            String refreshUrl = "https://ser.afm.gov.kz/kfm_new/api/v1/auth/refresh-access-token";

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

   @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        try {
            log.info("🧩 New signup attempt for email: {}", request.getEmail());

            // Check if email already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("User with this email already exists");
            }

            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // Create new user
            User newUser = new User();
            newUser.setUsername(request.getUsername());
            newUser.setEmail(request.getEmail());
            newUser.setPassword(encodedPassword);

            userRepository.save(newUser);
            log.info("✅ User created successfully: {}", newUser.getEmail());

            UserDetailsImpl userDetails = (UserDetailsImpl)
                    userDetailsService.loadUserByUsername(newUser.getUsername());
            String token = jwtTokenUtil.generateTokenFromUsername(userDetails.getUsername());

            return ResponseEntity.ok(new JwtResponse(
                    token,
                    newUser.getId(),
                    newUser.getUsername(),
                    newUser.getEmail(),
                    userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            ));

        } catch (Exception e) {
            log.error("❌ Error during signup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during signup: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SignUpRequest request) {
        try {
            log.info("🔐 Login attempt for username: {}", request.getUsername());

            var userOpt = userRepository.findByUsername(request.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid username or password");
            }

            User user = userOpt.get();

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid username or password");
            }

            // Load user details for roles and authorities
            UserDetailsImpl userDetails = (UserDetailsImpl)
                    userDetailsService.loadUserByUsername(user.getUsername());

            // Generate JWT token
            String token = jwtTokenUtil.generateTokenFromUsername(userDetails.getUsername());

            log.info("✅ Login successful for {}", user.getUsername());

            // Return standard JWT response
            return ResponseEntity.ok(new JwtResponse(
                    token,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            ));
        } catch (Exception e) {
            log.error("❌ Error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during login: " + e.getMessage());
        }
    }


}
