package com.example.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponse {
    private Integer id;
    private String code;
    private String title;
    private String description;
    private String imageUrl;
    private Integer categoryId;
    private String categoryTitle;
}
