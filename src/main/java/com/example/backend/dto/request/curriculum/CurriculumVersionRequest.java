package com.example.backend.dto.request.curriculum;

import com.example.backend.constant.CurriculumStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumVersionRequest {
    private Integer versionNo;
    private Integer basedOnVersionId;
    private CurriculumStatus status;

    @Valid
    private List<ChapterTemplateRequest> chapters;
}
