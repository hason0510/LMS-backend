package com.example.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private Integer id;
    private Integer studentId;
    private String userName;
    private String fullName;
    private String studentNumber;
    private String studentAvatar;
    private String courseTitle;
    private String courseCode;
    private Integer courseId;
    private Integer progress;
    private String approvalStatus;
}
