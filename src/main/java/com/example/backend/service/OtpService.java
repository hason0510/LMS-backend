package com.example.backend.service;

import com.example.backend.constant.OtpType;
import com.example.backend.entity.Otp;
import com.example.backend.entity.User;

public interface OtpService {
    void sendOtpEmail(String toGmail, String otpCode);

    Otp createOtp(User user, OtpType type);

    boolean validateOtp(User user, String code, OtpType type);

    void resendOtp(User user, OtpType type);
}
