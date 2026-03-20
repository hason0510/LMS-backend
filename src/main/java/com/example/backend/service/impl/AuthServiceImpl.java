package com.example.backend.service.impl;

import com.example.backend.constant.OtpType;
import com.example.backend.dto.request.*;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.user.UserInfoResponse;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuthService;
import com.example.backend.service.OtpService;
import com.example.backend.service.UserService;
import com.example.backend.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final OtpService otpService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthServiceImpl(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil, UserService userService, OtpService otpService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        User currentUser = userService.handleGetUserByUserName(request.getUsername());
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin();
        userLogin.setId(currentUser.getId());
        userLogin.setUsername(currentUser.getUserName());
        userLogin.setRole(String.valueOf(currentUser.getRole().getRoleName()));
        LoginResponse response = new LoginResponse();
        response.setUser(userLogin);
        // Generate tokens
        String accessToken = securityUtil.createAccessToken(authentication.getName(), response);
        String refreshToken = securityUtil.createRefreshToken(request.getUsername(), response);
        // Update refresh token in DB
        userService.updateUserToken(refreshToken, request.getUsername());
        // Set tokens in DTO
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    @Override
    public LoginResponse refreshToken(String oldRefreshToken){
        Jwt decodeToken = securityUtil.checkValidRefreshToken(oldRefreshToken);
        String userName = decodeToken.getSubject();
        User user = userService.handleGetUserByUserNameAndRefreshToken(userName, oldRefreshToken);
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                user.getId(),
                user.getUserName(),
                user.getRole().getRoleName().name()
        );
        LoginResponse LoginResponse = new LoginResponse();
        LoginResponse.setUser(userLogin);
        String accessToken = securityUtil.createAccessToken(userName, LoginResponse);
        LoginResponse.setAccessToken(accessToken);
        String newRefreshToken = securityUtil.createRefreshToken(userName, LoginResponse);
        LoginResponse.setRefreshToken(newRefreshToken); // This field is @JsonIgnore
        userService.updateUserToken(newRefreshToken, userName);
        return LoginResponse;
    }

    @Override
    public void logout() {
        String userName = userService.getCurrentUser().getUserName();
        userService.updateUserToken("", userName);
    }

    @Override
    public Integer register(RegisterRequest request) {
        UserInfoResponse userResponse = userService.registerUser(request);
        userService.initiateEmailVerification(request.getGmail());
        return userResponse.getId();
    }

    @Override
    public LoginResponse verifyOtp(OtpVerificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        boolean valid = otpService.validateOtp(
                user,
                request.getCode(),
                OtpType.EMAIL_VERIFICATION
        );
        if (!valid) {
            throw new BusinessException("OTP không hợp lệ hoặc đã hết hạn");
        }
        user.setVerified(true);
        userRepository.save(user);

        String roleName = user.getRole().getRoleName().name(); 
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUserName(), 
            null, 
            java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleName))
        );

        return buildLoginResponse(authentication, user);
    }

    @Override
    public void changePassWord(ChangePasswordRequest request){
        User user = userService.getCurrentUser();
        if(passwordEncoder.matches(request.getOldPassword(), user.getPassword())){
            if(request.getConfirmNewPassword().equals(request.getNewPassword())){
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
            }
            else throw new BusinessException("Hai mật khẩu không trùng khớp");
        }
        else throw new BusinessException("Mật khẩu bạn nhập không chính xác");
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("Hai mật khẩu không trùng khớp");
        }
        User user = userService.handleGetUserByGmail(request.getGmail());
        if (user == null) {
            throw new ResourceNotFoundException("Người dùng không tồn tại");
        }
        boolean validOtp = otpService.validateOtp(
                user,
                request.getOtp(),
                OtpType.PASSWORD_RESET
        );
        if (!validOtp) {
            throw new BusinessException("OTP không hợp lệ hoặc đã hết hạn");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void resetPasswordVerification(String gmail) {
        userService.resetPasswordVerification(gmail);
    }


    @Override
    public void resendRegisterOtp(String gmail) {
        User user = userService.handleGetUserByGmail(gmail);
        if (user == null) {
            throw new ResourceNotFoundException("Người dùng không tồn tại");
        }
        if (user.isVerified()) {
            throw new BusinessException("Tài khoản đã được xác thực");
        }
        otpService.resendOtp(user, OtpType.EMAIL_VERIFICATION);
    }

    @Override
    public void resendResetPasswordOtp(String gmail) {
        User user = userService.handleGetUserByGmail(gmail);
        if (user == null) {
            throw new ResourceNotFoundException("Người dùng không tồn tại");
        }
        otpService.resendOtp(user, OtpType.PASSWORD_RESET);
    }

    private LoginResponse buildLoginResponse(Authentication authentication, User user) {
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin();
        userLogin.setId(user.getId());
        userLogin.setUsername(user.getUserName());
        userLogin.setRole(user.getRole().getRoleName().name());
        LoginResponse response = new LoginResponse();
        response.setUser(userLogin);
        String accessToken =
                securityUtil.createAccessToken(authentication.getName(), response);
        String refreshToken =
                securityUtil.createRefreshToken(authentication.getName(), response);
        userService.updateUserToken(refreshToken, user.getUserName());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    @Override
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        try {
            // Parse Google token (simplified - in production should verify signature)
            String[] parts = request.getToken().split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid Google token format");
            }
            
            // Decode payload (second part)
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decodedBytes);
            
            // Parse JSON payload
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> claims = mapper.readValue(payload, java.util.Map.class);
            
            String email = (String) claims.get("email");
            String name = (String) claims.get("name");
            
            if (email == null) {
                throw new IllegalArgumentException("Email not found in token");
            }
            
            // Check if user exists, if not create new user automatically
            User googleUser = null;
            try {
                googleUser = userService.handleGetUserByGmail(email);
            } catch (Exception e) {
                // User doesn't exist, create new user automatically
                googleUser = userService.createGoogleUser(email, name != null ? name : email.split("@")[0]);
            }
            
            // Build response
            LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                    googleUser.getId(),
                    googleUser.getUserName(),
                    googleUser.getRole().getRoleName().name()
            );
            LoginResponse response = new LoginResponse();
            response.setUser(userLogin);
            
            // Generate tokens
            String accessToken = securityUtil.createAccessToken(email, response);
            String refreshToken = securityUtil.createRefreshToken(email, response);
            
            // Update refresh token in DB
            userService.updateUserToken(refreshToken, googleUser.getUserName());
            
            // Set tokens in response
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Google login failed: " + e.getMessage(), e);
        }
    }

}

