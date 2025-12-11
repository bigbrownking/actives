package org.info.infobaza.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.cache.interceptor.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {
    private final ObjectMapper mapper;

    @Bean("requestKeyGenerator")
    public KeyGenerator requestKeyGenerator() {
        return (target, method, params) -> {
            try {
                String json = mapper.writeValueAsString(params);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate cache key", e);
            }
        };
    }
}

