package com.example.backend.dto.request.quiz;

import com.example.backend.constant.QuestionContentBlockType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionContentBlockRequest {
    private QuestionContentBlockType blockType;
    private Integer orderIndex;
    private String content;
    private String language;
    private String blankKey;
    private Integer resourceId;
}
