package com.example.backend.dto.request.quiz;

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
public class QuizBankSourceRequest {
    private Integer id;
    private Integer questionBankId;
    private List<Integer> tagIds;
    private QuizTagMatchMode tagMatchMode;
    private QuizSourceSelectionMode selectionMode;
    private Integer questionCount;
    private DifficultyLevel difficultyLevel;
}
