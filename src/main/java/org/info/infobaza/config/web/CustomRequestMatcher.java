package org.info.infobaza.config.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class CustomRequestMatcher implements RequestMatcher {
    private final String allowedOrigin;

    public CustomRequestMatcher(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return origin != null && origin.equals(allowedOrigin);
    }
}
