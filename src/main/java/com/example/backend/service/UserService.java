package com.example.backend.service;

import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.UserCreateRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserInfoResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User handleGetUserByGmail(String email);

    User handleGetUserByUserName(String name);

    boolean isCurrentUser(Integer userId);

    User getCurrentUser();

    User handleGetUserByUserNameAndRefreshToken(String userName, String refreshToken);

    void updateUserToken(String refreshToken, String userName);

    void deleteUserById(Integer id);

    User createGoogleUser(String email, String name);

    UserInfoResponse updateUser(Integer id, RegisterRequest request);

    Object getUserById(Integer id);

    PageResponse<UserInfoResponse> getUserPage(Pageable pageable);

    UserInfoResponse registerUser(RegisterRequest request);

    UserInfoResponse createUser(UserCreateRequest request);

    PageResponse<UserInfoResponse> searchUser(SearchUserRequest request, Pageable pageable);

    void initiateEmailVerification(String gmail);

    void resetPasswordVerification(String gmail);

    UserInfoResponse convertUserInfoToDTO(User user);

    CloudinaryResponse uploadImage(final Integer id, final MultipartFile file);

    UserViewResponse convertUserViewToDTO(User user);

    // Object getUserById(Integer id);

    //UserInfoResponse updateUser(Integer id, UserRequest userRequest);
}
