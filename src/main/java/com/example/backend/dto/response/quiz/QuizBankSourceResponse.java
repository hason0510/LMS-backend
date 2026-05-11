package com.example.backend.dto.response.quiz;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuizSourceSelectionMode;
import com.example.backend.constant.QuizTagMatchMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizBankSourceResponse {
    private Integer id;
    private Integer questionBankId;
    private String questionBankName;
    private List<Integer> tagIds;
    private QuizTagMatchMode tagMatchMode;
    private Integer orderIndex;
    private QuizSourceSelectionMode selectionMode;
    private Integer questionCount;
    private DifficultyLevel difficultyLevel;
}
