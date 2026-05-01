package com.example.backend.dto.request.quiz;

import com.example.backend.constant.QuestionInteractionItemRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionInteractionItemRequest {
    private Integer id;
    private String content;
    private String itemKey;
    private QuestionInteractionItemRole role;
    private String correctMatchKey;
    private Integer correctOrderIndex;
    private Integer blankIndex;
    private List<String> acceptedAnswers;
    private Integer resourceId;
    private Integer orderIndex;
}
