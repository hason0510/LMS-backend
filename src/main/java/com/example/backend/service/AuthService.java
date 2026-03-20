package com.example.backend.service;

import com.example.backend.dto.request.*;
import com.example.backend.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(String oldRefreshToken);

    void logout();

    Integer register(RegisterRequest request);

    LoginResponse verifyOtp(OtpVerificationRequest request);

    void changePassWord(ChangePasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void resetPasswordVerification(String gmail);

    void resendRegisterOtp(String gmail);

    void resendResetPasswordOtp(String gmail);

    LoginResponse googleLogin(GoogleLoginRequest request);
}
