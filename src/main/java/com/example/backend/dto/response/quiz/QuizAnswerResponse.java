package com.example.backend.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerResponse {
    private Integer id;
    @JsonProperty("isCorrect")
    private Boolean isCorrect;
    private String content;
}
