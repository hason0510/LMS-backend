package com.example.backend.dto.response.quiz;

import com.example.backend.constant.QuestionType;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.quiz.QuestionContentBlockResponse;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponse {
    private Integer id;
    private Integer sourceBankQuestionId;
    private String content;
    private QuestionType type;
    private ResourceResponse resource;
    private Integer points;
    private List<QuizAnswerResponse> answers;
    private List<QuestionInteractionItemResponse> items;
    private List<QuestionContentBlockResponse> blocks;
}
