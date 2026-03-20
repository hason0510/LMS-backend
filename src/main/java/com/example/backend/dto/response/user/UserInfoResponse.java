package com.example.backend.dto.response.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Integer id;
    private String userName;
    private String fullName;
    private String phoneNumber;
    private String birthday;
    private String studentNumber;
    private String address;
    private String gmail;
    private String roleName;
    private String imageUrl;
    private String cloudinaryImageId;
    private String workPlace;
    private Integer yearsOfExperience;
    private String fieldOfExpertise;
    private String bio;

}