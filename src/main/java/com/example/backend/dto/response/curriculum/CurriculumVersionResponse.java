package com.example.backend.dto.response.curriculum;

import com.example.backend.constant.CurriculumStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumVersionResponse {
    private Integer id;
    private Integer versionNo;
    private CurriculumStatus status;
    private Integer templateId;
    private Integer basedOnVersionId;
    private Integer classSectionCount;
    private List<ChapterTemplateResponse> chapters;
}
