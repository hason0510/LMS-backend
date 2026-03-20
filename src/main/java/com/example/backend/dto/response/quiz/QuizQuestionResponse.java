package com.example.backend.dto.response.quiz;

import com.example.backend.constant.QuestionType;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponse {
    private Integer id;
    private String content;
    private QuestionType type;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private Integer points;
    private List<QuizAnswerResponse> answers;
}
