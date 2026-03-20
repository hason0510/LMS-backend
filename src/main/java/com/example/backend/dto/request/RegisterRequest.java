package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String userName;
    private String fullName;
    private String phoneNumber;
    private String birthday;
    private String address;
    private String gmail;
    private String roleName;
    private String password;
    private String studentNumber;
    private String workPlace;
    private Integer yearsOfExperience;
    private String fieldOfExpertise;
    private String bio;
}
