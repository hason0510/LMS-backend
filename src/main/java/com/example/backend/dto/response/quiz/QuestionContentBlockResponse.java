package com.example.backend.dto.response.quiz;

import com.example.backend.constant.QuestionContentBlockType;
import com.example.backend.dto.response.ResourceResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionContentBlockResponse {
    private Integer id;
    private QuestionContentBlockType blockType;
    private Integer orderIndex;
    private String content;
    private String language;
    private String blankKey;
    private ResourceResponse resource;
}
