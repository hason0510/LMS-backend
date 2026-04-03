package com.example.backend.dto.response.questionbank;

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
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private Integer orderIndex;
}
