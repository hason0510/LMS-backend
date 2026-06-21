package com.example.backend.dto.response.user;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse implements Serializable {
    private Integer id;
    private String userName;
    private String fullName;
    private String phoneNumber;
    private String birthday;
    private String studentNumber;
    private String address;
    private String gmail;
    private String roleName;
    private boolean active;
    private boolean googleLinked;
    private boolean localAuthEnabled;
    private String imageUrl;
    private String cloudinaryImageId;
    private String workPlace;
    private LocalDate joinDate;
    private String fieldOfExpertise;
    private String bio;
    private LocalDateTime createdDate;

}
