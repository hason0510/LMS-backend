package com.example.backend.dto.response.questionbank;

import com.example.backend.dto.response.ResourceResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankQuestionOptionResponse {
    private Integer id;
    private String content;
    private Boolean isCorrect;
    private String explanation;
    private Integer resourceId;
    private ResourceResponse resource;
    private Integer orderIndex;
}
