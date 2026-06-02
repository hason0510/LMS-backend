package com.example.backend.config;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.exception.AccountLockedException;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AccountStatusFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Integer userId = Integer.valueOf(jwt.getSubject());
            boolean isActive = userRepository.findById(userId)
                    .map(user -> user.isActive())
                    .orElse(false);

            if (!isActive) {
                SecurityContextHolder.clearContext();
                writeLockedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeLockedResponse(HttpServletResponse response) throws IOException {
        ApiResponse<Object> errorResponse = new ApiResponse<>(
                HttpStatus.FORBIDDEN.value(),
                AccountLockedException.ERROR_KEY,
                null
        );
        response.setContentType("application/json");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
