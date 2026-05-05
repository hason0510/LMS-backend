package com.example.backend.dto.response.quiz;

import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.dto.response.ResourceResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionInteractionItemResponse {
    private Integer id;
    private String content;
    private String itemKey;
    private QuestionInteractionItemRole role;
    private String correctMatchKey;
    private Integer correctOrderIndex;
    private Integer blankIndex;
    private List<String> acceptedAnswers;
    private String blankType;
    private String blankOptions;
    private Integer resourceId;
    private ResourceResponse resource;
    private Integer orderIndex;
}
