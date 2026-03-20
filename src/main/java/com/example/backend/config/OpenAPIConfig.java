package com.example.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI myOpenAPI() {
        return new OpenAPI()
                .info(createAPIInfo())
                .servers(List.of(
                        createServer("http://localhost:" + serverPort, 
                                    "Server URL in Development environment"),
                        createServer("https://api.your-lms-domain.com", 
                                    "Server URL in Production environment")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info createAPIInfo() {
        return new Info()
                .title("LMS (Learning Management System) API")
                .version("1.0")
                .description("""
                        This API exposes all endpoints for the Learning Management System.
                        """)
                .contact(new Contact()
                        .name("LMS Development Team")
                        .email("lms-support@example.com")
                        .url("https://github.com/your-lms-project"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private Server createServer(String url, String description) {
        return new Server()
                .url(url)
                .description(description);
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Enter JWT token. Format: Bearer {token}");
    }
}
