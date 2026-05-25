package com.example.backend.dto.response.classsection;

import com.example.backend.constant.ClassSectionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClassSectionJoinPreviewResponse {
    private Integer id;
    private String classCode;
    private String title;
    private String imageUrl;
    private ClassSectionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer subjectId;
    private String subjectCode;
    private String subjectTitle;
    private Integer categoryId;
    private String categoryTitle;
    private Integer teacherId;
    private String teacherName;
    private String teacherImageUrl;
    private Long totalEnrollments;
    private Boolean alreadyJoined;
    private String enrollmentStatus;
    private String joinMode;
    private String joinMessage;
}
