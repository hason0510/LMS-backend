package com.example.backend.dto.response.classsection;

import com.example.backend.constant.ClassSectionStatus;
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
    private String imageUrl;
    private ClassSectionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer subjectId;
    private String subjectTitle;
    private Integer teacherId;
    private String teacherName;
    private String teacherImageUrl;
    private List<ClassMemberResponse> teachingMembers;
    private String myClassRole;
    private String myWorkspaceType;
    private List<String> myCapabilities;
    private Integer curriculumTemplateId;
    private Boolean templateBased;
    private List<ClassChapterResponse> chapters;
    private Long totalEnrollments;
}
