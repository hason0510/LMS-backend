package com.example.backend.dto.request.questionbank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankQuestionOptionRequest {
    private String content;
    private Boolean isCorrect;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private Integer orderIndex;
}
