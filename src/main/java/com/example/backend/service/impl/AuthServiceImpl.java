package com.example.backend.service.impl;

import com.example.backend.constant.OtpType;
import com.example.backend.dto.request.*;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.user.UserInfoResponse;
import com.example.backend.entity.User;
import com.example.backend.exception.AccountLockedException;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuthService;
import com.example.backend.service.OtpService;
import com.example.backend.service.UserService;
import com.example.backend.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
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
        // Kiểm tra khóa tài khoản TRƯỚC khi authenticate. Nếu để loadUserByUsername
        // ném AccountLockedException trong lúc authenticate, DaoAuthenticationProvider
        // sẽ bọc nó thành InternalAuthenticationServiceException -> mất type -> client
        // chỉ nhận lỗi đăng nhập chung. Ném trực tiếp ở đây để giữ đúng ACCOUNT_LOCKED.
        User currentUser = userService.handleGetUserByUserName(request.getUsername());
        ensureAccountActive(currentUser);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
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
        userService.updateUserToken(refreshToken, currentUser.getUserName());
        // Set tokens in DTO
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    @Override
    public LoginResponse refreshToken(String oldRefreshToken) {
        if (!StringUtils.hasText(oldRefreshToken)) {
            throw new BadCredentialsException("Missing refresh token");
        }

        Jwt decodeToken;
        try {
            decodeToken = securityUtil.checkValidRefreshToken(oldRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid refresh token", e);
        }

        // 2. Lấy userId từ subject
        Integer userId;
        try {
            userId = Integer.valueOf(decodeToken.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Invalid refresh token", e);
        }

        // 3. Tìm user theo ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        ensureAccountActive(user);

        // 4. So sánh refresh token trong DB
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(oldRefreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // 5. Build response
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                user.getId(),
                user.getUserName(),
                user.getRole().getRoleName().name()
        );

        LoginResponse response = new LoginResponse();
        response.setUser(userLogin);

        // 6. Generate new tokens
        String newAccessToken = securityUtil.createAccessToken(user.getUserName(), response);
        String newRefreshToken = securityUtil.createRefreshToken(user.getUserName(), response);

        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);

        // 7. Rotate atomically (compare-and-swap). If another concurrent refresh
        // already rotated this token, 0 rows are affected -> reject this one.
        int rotated = userRepository.rotateRefreshToken(user.getId(), oldRefreshToken, newRefreshToken);
        if (rotated == 0) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        return response;
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
        ensureAccountActive(user);
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
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName))
        );

        return buildLoginResponse(authentication, user);
    }

    @Override
    public void changePassWord(ChangePasswordRequest request){
        User user = userService.getCurrentUser();
        validatePasswordChangeRequest(user, request.getOldPassword(), request.getNewPassword(), request.getConfirmNewPassword());
        otpService.resendOtp(user, OtpType.PASSWORD_CHANGE);
    }

    @Override
    public void confirmChangePassWord(ChangePasswordOtpRequest request) {
        User user = userService.getCurrentUser();
        validatePasswordChangeRequest(user, request.getOldPassword(), request.getNewPassword(), request.getConfirmNewPassword());
        boolean validOtp = otpService.validateOtp(
                user,
                request.getOtp(),
                OtpType.PASSWORD_CHANGE
        );
        if (!validOtp) {
            throw new BusinessException("OTP không hợp lệ hoặc đã hết hạn");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLocalAuthEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void resendChangePasswordOtp() {
        User user = userService.getCurrentUser();
        otpService.resendOtp(user, OtpType.PASSWORD_CHANGE);
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
        user.setLocalAuthEnabled(true);
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
        ensureAccountActive(user);
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
        String email;
        String name;
        // Only the Google token verification is wrapped; everything after must be
        // allowed to propagate so AccountLockedException reaches GlobalExceptionHandler.
        try {
            Jwt googleJwt = verifyGoogleIdToken(request.getToken());
            email = googleJwt.getClaimAsString("email");
            name = googleJwt.getClaimAsString("name");
        } catch (Exception e) {
            throw new RuntimeException("Google login failed: " + e.getMessage(), e);
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email not found in token");
        }

        User googleUser = userRepository.findFirstByGmailOrderByIdAsc(email).orElse(null);
        if (googleUser == null) {
            googleUser = userService.createGoogleUser(email, name != null ? name : email.split("@")[0]);
        } else if (!googleUser.isGoogleLinked() || !googleUser.isVerified()) {
            googleUser.setGoogleLinked(true);
            googleUser.setVerified(true);
            userRepository.save(googleUser);
        }
        // Locked accounts must be rejected with ACCOUNT_LOCKED, not a generic failure.
        ensureAccountActive(googleUser);

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
    }

    private void ensureAccountActive(User user) {
        if (user != null && !user.isActive()) {
            throw new AccountLockedException();
        }
    }

    private void validatePasswordChangeRequest(User user, String oldPassword, String newPassword, String confirmNewPassword) {
        if (!confirmNewPassword.equals(newPassword)) {
            throw new BusinessException("Hai mật khẩu không trùng khớp");
        }
        if (user.isLocalAuthEnabled()) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException("Mật khẩu bạn nhập không chính xác");
            }
            return;
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException("Mật khẩu mới không được để trống");
        }
    }

    private Jwt verifyGoogleIdToken(String idToken) {
        JwtDecoder jwtDecoder = buildGoogleJwtDecoder();
        Jwt jwt = jwtDecoder.decode(idToken);
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google token does not contain email");
        }
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new BadCredentialsException("Google account email is not verified");
        }
        return jwt;
    }

    private JwtDecoder buildGoogleJwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audience = token.getAudience();
            if (audience != null && audience.contains(googleClientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Google audience", null));
        };
        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String issuer = token.getIssuer() != null ? token.getIssuer().toString() : null;
            if ("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Google issuer", null));
        };
        jwtDecoder.setJwtValidator(token -> {
            OAuth2TokenValidatorResult defaults = JwtValidators.createDefault().validate(token);
            if (defaults.hasErrors()) {
                return defaults;
            }
            OAuth2TokenValidatorResult issuerResult = issuerValidator.validate(token);
            if (issuerResult.hasErrors()) {
                return issuerResult;
            }
            return audienceValidator.validate(token);
        });
        return jwtDecoder;
    }

}

