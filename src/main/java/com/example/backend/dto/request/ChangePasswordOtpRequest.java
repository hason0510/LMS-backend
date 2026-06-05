package com.example.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordOtpRequest {
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
    private String otp;
}
