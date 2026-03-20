package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRequest {
    private String title;
    private String startTime;
    private String endTime;
    private Integer courseId;
}
