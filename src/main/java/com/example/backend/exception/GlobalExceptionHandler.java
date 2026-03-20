package com.example.backend.exception;

import com.example.backend.dto.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllException(Exception ex) {
        if (ex instanceof org.springframework.security.core.AuthenticationException
                || ex instanceof org.springframework.security.access.AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        res.setData(ex.getMessage());
        res.setMessage("Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidArgument(MethodArgumentNotValidException exception) {
        Map<String, String> errorMap = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errorMap.put(error.getField(), error.getDefaultMessage());
        });
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(HttpStatus.BAD_REQUEST.value());
        res.setMessage("Validation failed");
        res.setData(errorMap);
        return ResponseEntity.badRequest().body(res);
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> DataIntegrityViolationException(DataIntegrityViolationException e) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(HttpStatus.BAD_REQUEST.value());
        res.setMessage(e.getMessage());
        res.setData(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> ResourceNotFoundException(ResourceNotFoundException e) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(HttpStatus.NOT_FOUND.value());
        res.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> UnauthorizedException(UnauthorizedException e) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(HttpStatus.FORBIDDEN.value());
        res.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
    }

    @ExceptionHandler(value = BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> BusinessException(BusinessException e) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(HttpStatus.BAD_REQUEST.value());
        res.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }
}
