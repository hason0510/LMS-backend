package com.example.backend.config;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.exception.AccountLockedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException{
        boolean locked = AccountLockedException.ERROR_KEY.equals(authException.getMessage());
        int status = locked ? HttpStatus.FORBIDDEN.value() : HttpStatus.UNAUTHORIZED.value();
        String message = locked ? AccountLockedException.ERROR_KEY : "Incorrect username or password";
        ApiResponse<?> errorResponse = new ApiResponse<>(
                status, message, null
        );
        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
