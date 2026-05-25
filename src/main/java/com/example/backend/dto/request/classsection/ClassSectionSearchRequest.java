package com.example.backend.dto.request.classsection;

import com.example.backend.constant.ClassSectionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClassSectionSearchRequest {
    private String keyword;
    private String teacherKeyword;
    private String subjectKeyword;
    private Integer categoryId;
    private Integer subjectId;
    private ClassSectionStatus status;
    private LocalDate startDateFrom;
    private LocalDate startDateTo;
    private String scope;
    private Integer pageNumber;
    private Integer pageSize;
    private String sortBy;
    private String sortDirection;
}
