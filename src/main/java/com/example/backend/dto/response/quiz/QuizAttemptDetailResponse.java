package com.example.backend.dto.response.quiz;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptDetailResponse extends QuizAttemptResponse{
    private List<QuizAttemptAnswerResponse> answers;
}
