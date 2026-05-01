package com.example.backend.dto.request.quiz;

import com.example.backend.constant.QuestionType;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionRequest {
    private Integer id;
    private String content;
    private QuestionType type;
    private Integer points;
    private Integer resourceId;
    private List<QuizAnswerRequest> answers;
    private List<QuestionInteractionItemRequest> items;
    private List<QuestionContentBlockRequest> blocks;
}
