package com.example.backend.dto.response.user;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserViewResponse {
    private Integer id;
    private String userName;
    private String fullName;
    private String studentNumber;
    private String gmail;
    private boolean googleLinked;
    private boolean localAuthEnabled;
    private String imageUrl;
    private String cloudinaryImageId;
    private String workPlace;
    private LocalDate joinDate;
    private String fieldOfExpertise;
    private String bio;
}
