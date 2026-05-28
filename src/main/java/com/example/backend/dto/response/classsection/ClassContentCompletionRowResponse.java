package com.example.backend.dto.response.classsection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassContentCompletionRowResponse {
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private String email;
    private String avatarUrl;
    private Boolean completed;
    private java.time.LocalDateTime completedAt;
}
