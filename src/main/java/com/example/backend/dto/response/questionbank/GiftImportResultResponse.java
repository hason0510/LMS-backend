package com.example.backend.dto.response.questionbank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GiftImportResultResponse {
    private Integer totalQuestions;
    private Integer importedQuestions;
    private Integer skippedQuestions;
    private List<String> warnings;
}
