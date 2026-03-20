package com.example.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private Integer id;
    private String roomCode;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String courseTitle;
}
