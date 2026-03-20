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
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private List<QuizAnswerRequest> answers;
}
