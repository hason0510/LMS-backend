package com.example.backend.config;

import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.user.UserInfoResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import com.example.backend.service.impl.UserServiceImpl;
import com.example.backend.utils.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    @Value("${hayson.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public GoogleOAuth2SuccessHandler(UserServiceImpl userService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.securityUtil = securityUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        User googleUser = userService.handleGetUserByGmail(email);
        if (googleUser == null) {
            googleUser =  userService.createGoogleUser(email, name);
        }
        // Build JWTs
        assert googleUser != null;
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                googleUser.getId(),
                googleUser.getUserName(),
                googleUser.getRole().getRoleName().name()
        );
        LoginResponse responseDTO = new LoginResponse();
        responseDTO.setUser(userLogin);

        String accessToken = securityUtil.createAccessToken(email, responseDTO);
        String refreshToken = securityUtil.createRefreshToken(email, responseDTO);
        userService.updateUserToken(refreshToken, email);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        responseDTO.setAccessToken(accessToken); // access token returned in JSON
        new ObjectMapper().writeValue(response.getWriter(), responseDTO);

    }
}

