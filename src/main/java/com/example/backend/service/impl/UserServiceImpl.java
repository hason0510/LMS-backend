package com.example.backend.service.impl;

import com.example.backend.constant.OtpType;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.UserCreateRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserInfoResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.Otp;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.service.OtpService;
import com.example.backend.service.UserService;
import com.example.backend.specification.UserSpecification;
import com.example.backend.utils.FileUploadUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Random;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final OtpService otpService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromGmail;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, CloudinaryService cloudinaryService, OtpService otpService, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
        this.otpService = otpService;
        this.mailSender = mailSender;
    }

    @Override
    public User handleGetUserByGmail(String email) {
        User user = userRepository.findByGmail(email); // trả về User, có thể null
        if (user == null) {
            throw new ResourceNotFoundException("Người dùng không tồn tại");
        }
        return user;
    }

    @Override
    public User handleGetUserByUserName(String username) {
        User user = userRepository.findByUserName(username); // trả về User, có thể null
        if (user == null) {
            throw new ResourceNotFoundException("Người dùng không tồn tại");
        }
        return user;
    }


    @Override
    public boolean isCurrentUser(Integer userId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            Integer currentUserId = Integer.valueOf(jwt.getSubject()); // sub = userId
            return userId.equals(currentUserId);
        }
        return false;
    }

    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            Integer currentUserId = Integer.valueOf(jwt.getSubject()); // sub = userId
            return userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        return null;
    }


    @Override
    public User handleGetUserByUserNameAndRefreshToken(String userName, String refreshToken) {
        return userRepository.findByUserNameAndRefreshToken(userName, refreshToken);
    }

    @Override
    public void updateUserToken(String refreshToken, String userName) {
        User currentUser = handleGetUserByUserName(userName);
        if(currentUser != null) {
            currentUser.setRefreshToken(refreshToken);
            userRepository.save(currentUser);
        }
    }

    @Override
    public void deleteUserById(Integer id) {
        User user = userRepository.findById(id).orElse(null);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }
        if(isCurrentUser(id) || getCurrentUser().getRole().getRoleName().equals(RoleType.ADMIN)) {
            userRepository.deleteById(id);
        }
    }

    @Override
    public User createGoogleUser(String email, String username) {
        User googleUser = User.builder()
                .userName(email)
                .password("123")
                .fullName(username)
                .gmail(email)
                .role(roleRepository.findByRoleName(RoleType.STUDENT))
                .isVerified(true)
                .build();
        return userRepository.save(googleUser);
    }


    @Override
    public UserInfoResponse updateUser(Integer id, RegisterRequest request) {
        User updatedUser = userRepository.findById(id).orElse(null);

        if(!isCurrentUser(id) && !getCurrentUser().getRole().getRoleName().equals(RoleType.ADMIN) ){
            throw new UnauthorizedException("You have no permission");
        }
        if(updatedUser == null){
            throw new ResourceNotFoundException("User not found");
        }

        if (request.getUserName() != null && !request.getUserName().equals(updatedUser.getUserName())) {
            if(userRepository.findByUserName(request.getUserName()) != null){
                throw new BusinessException("Tên người dùng đã được sử dụng, vui lòng chọn tên khác");
            } else updatedUser.setUserName(request.getUserName());
        } else if(request.getUserName() != null){
            updatedUser.setUserName(request.getUserName());
        } else{
            updatedUser.setUserName(updatedUser.getUserName());
        }

        if(request.getStudentNumber() != null && !request.getStudentNumber().equals(updatedUser.getStudentNumber())) {
            if(userRepository.findByStudentNumber(request.getStudentNumber()) != null){
                throw new BusinessException("Mã số này đã được sử dụng");
            } else updatedUser.setStudentNumber(request.getStudentNumber());
        } else if(request.getStudentNumber() != null){
            updatedUser.setStudentNumber(request.getStudentNumber());
        } else updatedUser.setStudentNumber(updatedUser.getStudentNumber());

        if(request.getGmail() != null && !request.getGmail().equals(updatedUser.getGmail())) {
            if(userRepository.findByGmail(request.getGmail()) != null){
                throw new BusinessException("Gmail này đã được sử dụng");
            } else updatedUser.setGmail(request.getGmail());
        } else if(request.getGmail() != null){
            updatedUser.setGmail(request.getGmail());
        }

        if (request.getBirthday() != null) {
            updatedUser.setBirthday(request.getBirthday());
        }
        if (request.getAddress() != null) {
            updatedUser.setAddress(request.getAddress());
        }
        if (request.getPhoneNumber() != null) {
            updatedUser.setPhoneNumber(request.getPhoneNumber());
        } else updatedUser.setPhoneNumber(updatedUser.getPhoneNumber());
        
        if (request.getFullName() != null) {
            updatedUser.setFullName(request.getFullName());
        }
        
        // Update teacher-specific fields
        if (request.getWorkPlace() != null) {
            updatedUser.setWorkPlace(request.getWorkPlace());
        }
        if (request.getYearsOfExperience() != null) {
            updatedUser.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getFieldOfExpertise() != null) {
            updatedUser.setFieldOfExpertise(request.getFieldOfExpertise());
        }
        if (request.getBio() != null) {
            updatedUser.setBio(request.getBio());
        }

        userRepository.save(updatedUser);
        return convertUserInfoToDTO(updatedUser);
    }

    @Override
    public Object getUserById(Integer id){
        User user = userRepository.findById(id).orElse(null);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }
        return convertUserInfoToDTO(user);
    }

    @Transactional
    public CloudinaryResponse uploadImage(final Integer id, final MultipartFile file) {
        final User avatarUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FileUploadUtil.assertAllowed(file, "image");
        final String cloudinaryImageId = avatarUser.getCloudinaryImageId();
        if(StringUtils.hasText(cloudinaryImageId)) {
            cloudinaryService.deleteFile(cloudinaryImageId, ResourceType.IMAGE);
        }
        final String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        final CloudinaryResponse response = this.cloudinaryService.uploadFile(file, fileName, "image");
        avatarUser.setImageUrl(response.getUrl());
        avatarUser.setCloudinaryImageId(response.getPublicId());
        userRepository.save(avatarUser);
        return response;
    }

    @Override
    public PageResponse<UserInfoResponse> getUserPage(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        Page<UserInfoResponse> userResponse = userPage.map(this::convertUserInfoToDTO);
        PageResponse<UserInfoResponse> pageDTO = new PageResponse<>(
                userResponse.getNumber() + 1,
                userResponse.getTotalPages(),
                userResponse.getNumberOfElements(),
                userResponse.getContent()
        );
        return pageDTO;
    }

    @Override
    public UserInfoResponse registerUser(RegisterRequest request){
        User user = new User();
        if(userRepository.findByUserName(request.getUserName()) != null){
            throw new BusinessException("Tên người dùng đã được sử dụng, vui lòng chọn tên khác");
        } else user.setUserName(request.getUserName());

        if(userRepository.findByGmail(request.getGmail()) != null){
            throw new BusinessException("Gmail này đã được sử dụng");
        } else user.setGmail(request.getGmail());

        if(userRepository.findByStudentNumber(request.getStudentNumber()) != null){
            throw new BusinessException("Mã số này đã được sử dụng");
        } else user.setStudentNumber(request.getStudentNumber());

        user.setRole(roleRepository.findByRoleName(RoleType.valueOf(request.getRoleName())));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setFullName(request.getFullName());
        user.setVerified(false);
        
        // Set teacher-specific fields if provided
        if (request.getWorkPlace() != null) {
            user.setWorkPlace(request.getWorkPlace());
        }
        if (request.getYearsOfExperience() != null) {
            user.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getFieldOfExpertise() != null) {
            user.setFieldOfExpertise(request.getFieldOfExpertise());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        userRepository.save(user);
        return convertUserInfoToDTO(user);
    }

    @Override
    public UserInfoResponse createUser(UserCreateRequest request){
        User user = new User();
        if(userRepository.findByUserName(request.getUserName()) != null){
            throw new BusinessException("Tên người dùng đã được sử dụng, vui lòng chọn tên khác");
        } else user.setUserName(request.getUserName());

        if(userRepository.findByGmail(request.getGmail()) != null){
            throw new BusinessException("Gmail này đã được sử dụng");
        } else user.setGmail(request.getGmail());

        if(userRepository.findByStudentNumber(request.getStudentNumber()) != null){
            throw new BusinessException("Mã số này đã được sử dụng");
        } else user.setStudentNumber(request.getStudentNumber());

        // Generate random password
        String generatedPassword = generateRandomPassword();
        
        user.setRole(roleRepository.findByRoleName(RoleType.valueOf(request.getRoleName())));
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setFullName(request.getFullName());
        user.setVerified(true);
        userRepository.save(user);
        
        // Send password to email
        sendPasswordEmail(user.getGmail(), generatedPassword);
        
        return convertUserInfoToDTO(user);
    }

    @Override
    public PageResponse<UserInfoResponse> searchUser(SearchUserRequest request, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(request.getUserName())) {
            spec = spec.and(UserSpecification.likeUserName(request.getUserName()));
        }
        if (StringUtils.hasText(request.getFullName())) {
            spec = spec.and(UserSpecification.likeFullName(request.getFullName()));
        }
        if (StringUtils.hasText(request.getStudentNumber())) {
            spec = spec.and(UserSpecification.hasStudentNumber(request.getStudentNumber()));
        }
        if (StringUtils.hasText(request.getGmail())) {
            spec = spec.and(UserSpecification.likeGmail(request.getGmail()));
        }
        if (StringUtils.hasText(request.getRoleName())) {
            RoleType roleType = RoleType.valueOf(request.getRoleName().toUpperCase());
            spec = spec.and(UserSpecification.hasRole(roleType));
        }
        Page<User> userPage = userRepository.findAll(spec, pageable);
        Page<UserInfoResponse> response = userPage.map(this::convertUserInfoToDTO);
        return new PageResponse<>(
                response.getNumber() + 1,
                response.getNumberOfElements(),
                response.getTotalPages(),
                response.getContent()
        );
    }

    @Override
    public void initiateEmailVerification(String gmail) {
        User user = handleGetUserByGmail(gmail);
        Otp otp = otpService.createOtp(user, OtpType.EMAIL_VERIFICATION);
        otpService.sendOtpEmail(user.getGmail(), otp.getCode());
    }

    @Override
    public void resetPasswordVerification(String gmail) {
        User user = handleGetUserByGmail(gmail);
        Otp otp = otpService.createOtp(user, OtpType.PASSWORD_RESET);
        otpService.sendOtpEmail(user.getGmail(), otp.getCode());
    }

    /**
     * Generate a random password with 12 characters
     * Contains uppercase, lowercase, digits, and special characters
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    /**
     * Send password to user email
     */
    private void sendPasswordEmail(String toGmail, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromGmail);
            message.setTo(toGmail);
            message.setSubject("Thông tin tài khoản của bạn");
            message.setText("Chào bạn,\n\n" +
                    "Tài khoản của bạn đã được tạo thành công.\n" +
                    "Mật khẩu tạm thời của bạn là: " + password + "\n\n" +
                    "Vui lòng đăng nhập và đổi mật khẩu khi lần đầu sử dụng.\n\n" +
                    "Trân trọng,\n" +
                    "Hệ thống LMS");
            mailSender.send(message);
        } catch (Exception e) {
            // Log error but don't fail the user creation if email sending fails
            System.err.println("Failed to send password email to " + toGmail + ": " + e.getMessage());
        }
    }

    @Override
    public UserInfoResponse convertUserInfoToDTO(User user){
        UserInfoResponse userDTO = new UserInfoResponse();
        userDTO.setId(user.getId());
        userDTO.setUserName(user.getUserName());
        userDTO.setBirthday(user.getBirthday());
        userDTO.setStudentNumber(user.getStudentNumber());
        userDTO.setAddress(user.getAddress());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setFullName(user.getFullName());
        userDTO.setGmail(user.getGmail());
        userDTO.setRoleName(user.getRole().getRoleName().toString());
        userDTO.setImageUrl(user.getImageUrl());
        userDTO.setCloudinaryImageId(user.getCloudinaryImageId());
        userDTO.setWorkPlace(user.getWorkPlace());
        userDTO.setYearsOfExperience(user.getYearsOfExperience());
        userDTO.setFieldOfExpertise(user.getFieldOfExpertise());
        userDTO.setBio(user.getBio());
        return userDTO;
    }

    @Override
    public UserViewResponse convertUserViewToDTO(User user){
        UserViewResponse userDTO = new UserViewResponse();
        userDTO.setId(user.getId());
        userDTO.setUserName(user.getUserName());
        userDTO.setStudentNumber(user.getStudentNumber());
        userDTO.setFullName(user.getFullName());
        userDTO.setGmail(user.getGmail());
        userDTO.setImageUrl(user.getImageUrl());
        userDTO.setCloudinaryImageId(user.getCloudinaryImageId());
        userDTO.setWorkPlace(user.getWorkPlace());
        userDTO.setYearsOfExperience(user.getYearsOfExperience());
        userDTO.setFieldOfExpertise(user.getFieldOfExpertise());
        userDTO.setBio(user.getBio());
        return userDTO;
    }



}
