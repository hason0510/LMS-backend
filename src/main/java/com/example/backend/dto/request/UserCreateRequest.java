package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    private String userName;
    private String fullName;
    private String phoneNumber;
    private String birthday;
    private String address;
    private String gmail;
    private String roleName;
    private String password;
    private String studentNumber;
}
