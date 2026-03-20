package com.example.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String gmail;
    private String otp;
    private String newPassword;
    private String confirmNewPassword;
}

