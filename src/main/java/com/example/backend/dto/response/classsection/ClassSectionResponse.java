package com.example.backend.dto.response.classsection;

import com.example.backend.constant.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSectionResponse {
    private Integer id;
    private String classCode;
    private String title;
    private String description;
    private CourseStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer subjectId;
    private String subjectTitle;
    private Integer teacherId;
    private String teacherName;
    private Integer managerId;
    private String managerName;
    private Integer curriculumVersionId;
    private Integer legacyCourseId;
    private Boolean templateBased;
    private Boolean migratedFromLegacyCourse;
    private List<ClassChapterResponse> chapters;
}
