package com.example.backend.utils;

import com.example.backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class FormatApiResponse implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        HttpServletResponse ServletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = ServletResponse.getStatus();
        if (body instanceof String || body instanceof Resource) {
            return body;
        }
        ApiResponse<Object> res = new ApiResponse<>();
        res.setCode(status);
        if (status >= 400) {
            return body;
        }
        if (request.getURI().getPath().contains("/swagger") ||
                request.getURI().getPath().contains("/v3/api-docs")) {
            return body;
        }
        res.setMessage("Call API Success");
        res.setData(body);
        return res;

    }
}

