package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    private Integer userId;
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}
