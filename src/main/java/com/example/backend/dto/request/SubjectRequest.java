package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequest {
    private String code;
    private String title;
    private String description;
    private Integer categoryId;
}
