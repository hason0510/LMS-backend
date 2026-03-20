package com.example.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BackendApplication {

    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU KIỂM TRA BIẾN MÔI TRƯỜNG ===");

        // 1. Kiểm tra xem Windows có đang lưu lén biến này không
        System.out.println("Từ Windows OS: " + System.getenv("DATASOURCE_URL"));

        // Load Dotenv của bạn
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        // 2. Kiểm tra xem file .env mà Java ĐỌC ĐƯỢC thực chất đang chứa gì
        System.out.println("Từ file .env: " + System.getProperty("DATASOURCE_URL"));

        System.out.println("=========================================");

        SpringApplication.run(BackendApplication.class, args);
    }

}
