package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Configuration
public class AuditorConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = auth.getPrincipal();
            if (principal instanceof Jwt jwt) {
                Object userClaim = jwt.getClaim("user");
                if (userClaim instanceof Map<?, ?> userMap) {
                    Object username = userMap.get("username");
                    if (username instanceof String value && StringUtils.hasText(value)) {
                        return Optional.of(value);
                    }
                }

                String username = jwt.getClaimAsString("username");
                if (StringUtils.hasText(username)) {
                    return Optional.of(username);
                }

                if (StringUtils.hasText(jwt.getSubject())) {
                    return Optional.of(jwt.getSubject());
                }
            }

            if (!StringUtils.hasText(auth.getName()) || "anonymousUser".equalsIgnoreCase(auth.getName())) {
                return Optional.empty();
            }
            return Optional.of(auth.getName());
        };
    }
}
