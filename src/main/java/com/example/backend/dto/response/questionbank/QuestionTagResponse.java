package com.example.backend.dto.response.questionbank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTagResponse {
    private Integer id;
    private String name;
    private Integer subjectId;
}
