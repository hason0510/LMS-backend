package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceReferenceResponse {
    private String entityType;
    private Integer entityId;
    private String fieldName;
    private String label;
    private String contextPath;
    private Integer classSectionId;
    private String classSectionTitle;
    private Integer quizId;
    private String quizTitle;
    private Integer questionBankId;
    private String questionBankName;
    private Integer subjectId;
    private String subjectTitle;
}
