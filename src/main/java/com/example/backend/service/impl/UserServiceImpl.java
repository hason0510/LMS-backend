package com.example.backend.service.impl;

import com.example.backend.cache.CacheNames;
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
import com.example.backend.entity.Role;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
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
        return userRepository.findFirstByGmailOrderByIdAsc(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
    }

    @Override
    public User handleGetUserByUserName(String username) {
        return userRepository.findFirstByUserNameOrderByIdAsc(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
    }

    @Override
    public User handleGetUserByLoginIdentifier(String identifier) {
        if (identifier != null && identifier.contains("@")) {
            return handleGetUserByGmail(identifier);
        }
        return handleGetUserByUserName(identifier);
    }

    @Override
    public boolean isCurrentUser(Integer userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Integer currentUserId = Integer.valueOf(jwt.getSubject());
            return userId.equals(currentUserId);
        }
        return false;
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Integer currentUserId = Integer.valueOf(jwt.getSubject());
            return userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        return null;
    }

    @Override
    public User handleGetUserByUserNameAndRefreshToken(String userName, String refreshToken) {
        return userRepository.findFirstByUserNameAndRefreshTokenOrderByIdAsc(userName, refreshToken).orElse(null);
    }

    @Override
    public void updateUserToken(String refreshToken, String userName) {
        User currentUser = handleGetUserByUserName(userName);
        if (currentUser != null) {
            currentUser.setRefreshToken(refreshToken);
            userRepository.save(currentUser);
        }
    }

    private void assertValidUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            throw new BusinessException("Tên người dùng không được để trống");
        }
        if (userName.trim().contains("@")) {
            throw new BusinessException("Tên người dùng không được chứa ký tự @");
        }
    }

    private String generateUniqueGoogleUserName(String email) {
        String emailLocalPart = email.split("@")[0]
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "");
        String base = StringUtils.hasText(emailLocalPart) ? emailLocalPart : "googleuser";
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUserName(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#id"),
            @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    })
    public void deleteUserById(Integer id) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser == null || currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("You have no permission");
        }
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("Bạn không thể tự xóa tài khoản của mình");
        }
        if (targetUser.getRole() != null && targetUser.getRole().getRoleName() == RoleType.ADMIN) {
            throw new BusinessException("Không thể xóa tài khoản quản trị viên trong phiên bản này");
        }

        // Đá phiên đang đăng nhập (nếu có) trước khi xóa mềm
        targetUser.setRefreshToken(null);
        userRepository.save(targetUser);
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#id"),
            @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    })
    public UserInfoResponse lockUser(Integer id) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser == null || currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("You have no permission");
        }
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("Bạn không thể tự khóa tài khoản của mình");
        }
        if (targetUser.getRole() != null && targetUser.getRole().getRoleName() == RoleType.ADMIN) {
            throw new BusinessException("Không thể khóa tài khoản quản trị viên trong phiên bản này");
        }

        targetUser.setActive(false);
        targetUser.setRefreshToken(null);
        userRepository.save(targetUser);
        return convertUserInfoToDTO(targetUser);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#id"),
            @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    })
    public UserInfoResponse unlockUser(Integer id) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser == null || currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("You have no permission");
        }
        if (targetUser.getRole() != null && targetUser.getRole().getRoleName() == RoleType.ADMIN) {
            throw new BusinessException("Không thể mở khóa tài khoản quản trị viên trong phiên bản này");
        }

        targetUser.setActive(true);
        userRepository.save(targetUser);
        return convertUserInfoToDTO(targetUser);
    }

    @Override
    @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    public User createGoogleUser(String email, String username) {
        String generatedUserName = generateUniqueGoogleUserName(email);
        User googleUser = User.builder()
                .userName(generatedUserName)
                .password(null)
                .fullName(username)
                .gmail(email)
                .role(getRequiredRole(RoleType.STUDENT))
                .googleLinked(true)
                .localAuthEnabled(false)
                .isVerified(true)
                .build();
        return userRepository.save(googleUser);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#id"),
            @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    })
    public UserInfoResponse updateUser(Integer id, RegisterRequest request) {
        User updatedUser = userRepository.findById(id).orElse(null);
        boolean isAdmin = getCurrentUser().getRole().getRoleName().equals(RoleType.ADMIN);

        if (!isCurrentUser(id) && !isAdmin) {
            throw new UnauthorizedException("You have no permission");
        }
        if (updatedUser == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (request.getUserName() != null) {
            String nextUserName = request.getUserName().trim();
            assertValidUserName(nextUserName);
            if (!nextUserName.equals(updatedUser.getUserName()) && userRepository.existsByUserName(nextUserName)) {
                throw new BusinessException("Tên người dùng đã được sử dụng, vui lòng chọn tên khác");
            }
            updatedUser.setUserName(nextUserName);
        }

        if (request.getStudentNumber() != null) {
            String nextStudentNumber = request.getStudentNumber().trim();
            if (!isAdmin && !nextStudentNumber.equals(updatedUser.getStudentNumber())) {
                throw new UnauthorizedException("You have no permission");
            }
            if (!nextStudentNumber.equals(updatedUser.getStudentNumber())) {
                if (userRepository.existsByStudentNumber(nextStudentNumber)) {
                    throw new BusinessException("Mã số này đã được sử dụng");
                } else {
                    updatedUser.setStudentNumber(nextStudentNumber);
                }
            }
        }

        if (request.getGmail() != null && !request.getGmail().equals(updatedUser.getGmail())) {
            if (updatedUser.isVerified()) {
                throw new BusinessException("Không thể thay đổi email sau khi tài khoản đã được xác thực");
            }
            String nextGmail = request.getGmail().trim();
            if (userRepository.existsByGmail(nextGmail)) {
                throw new BusinessException("Gmail này đã được sử dụng");
            } else {
                updatedUser.setGmail(nextGmail);
            }
        } else if (request.getGmail() != null) {
            updatedUser.setGmail(request.getGmail().trim());
        }

        if (request.getImageUrl() != null) {
            final String nextImageUrl = request.getImageUrl().trim();
            final String currentImageUrl = updatedUser.getImageUrl();
            final boolean imageUrlChanged = !StringUtils.hasText(nextImageUrl)
                    ? StringUtils.hasText(currentImageUrl)
                    : !nextImageUrl.equals(currentImageUrl);

            if (imageUrlChanged && StringUtils.hasText(updatedUser.getCloudinaryImageId())) {
                cloudinaryService.deleteFile(updatedUser.getCloudinaryImageId(), ResourceType.IMAGE);
            }

            if (StringUtils.hasText(nextImageUrl)) {
                updatedUser.setImageUrl(nextImageUrl);
            } else {
                updatedUser.setImageUrl(null);
            }

            if (imageUrlChanged) {
                updatedUser.setCloudinaryImageId(null);
            }
        }

        if (request.getBirthday() != null) {
            updatedUser.setBirthday(request.getBirthday());
        }
        if (request.getAddress() != null) {
            updatedUser.setAddress(request.getAddress());
        }
        if (request.getPhoneNumber() != null) {
            updatedUser.setPhoneNumber(request.getPhoneNumber());
        } else {
            updatedUser.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (request.getFullName() != null) {
            updatedUser.setFullName(request.getFullName());
        }
        if (request.getWorkPlace() != null) {
            updatedUser.setWorkPlace(request.getWorkPlace());
        }
        if (request.getJoinDate() != null && isAdmin) {
            updatedUser.setJoinDate(request.getJoinDate());
        }
        if (request.getFieldOfExpertise() != null) {
            updatedUser.setFieldOfExpertise(request.getFieldOfExpertise());
        }
        if (request.getBio() != null) {
            updatedUser.setBio(request.getBio());
        }

        userRepository.saveAndFlush(updatedUser);

        User persistedUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return convertUserInfoToDTO(persistedUser);
    }

    @Override
    @Cacheable(value = CacheNames.USER, key = "#id")
    public Object getUserById(Integer id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return convertUserInfoToDTO(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#id"),
            @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    })
    public CloudinaryResponse uploadImage(final Integer id, final MultipartFile file) {
        final User avatarUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!isCurrentUser(id) && !getCurrentUser().getRole().getRoleName().equals(RoleType.ADMIN)) {
            throw new UnauthorizedException("You have no permission");
        }
        FileUploadUtil.assertAllowed(file, "image");
        final String cloudinaryImageId = avatarUser.getCloudinaryImageId();
        if (StringUtils.hasText(cloudinaryImageId)) {
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
    @Cacheable(value = CacheNames.USER_PAGE, key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public PageResponse<UserInfoResponse> getUserPage(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        Page<UserInfoResponse> userResponse = userPage.map(this::convertUserInfoToDTO);
        return new PageResponse<>(
                userResponse.getNumber() + 1,
                userResponse.getTotalPages(),
                userResponse.getTotalElements(),
                userResponse.getContent()
        );
    }

    @Override
    @Caching(
            put = @CachePut(value = CacheNames.USER, key = "#result.id"),
            evict = @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    )
    public UserInfoResponse registerUser(RegisterRequest request) {
        User user = new User();
        assertValidUserName(request.getUserName());
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new BusinessException("Tên người dùng đã được sử dụng, vui lòng chọn tên khác");
        } else {
            user.setUserName(request.getUserName());
        }

        if (userRepository.existsByGmail(request.getGmail())) {
            throw new BusinessException("Gmail này đã được sử dụng");
        } else {
            user.setGmail(request.getGmail());
        }

        if (userRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new BusinessException("Mã số này đã được sử dụng");
        } else {
            user.setStudentNumber(request.getStudentNumber());
        }

        user.setRole(getRequiredRole(RoleType.STUDENT));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setLocalAuthEnabled(true);
        user.setGoogleLinked(false);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setFullName(request.getFullName());
        user.setVerified(false);

        if (request.getWorkPlace() != null) {
            user.setWorkPlace(request.getWorkPlace());
        }
        if (request.getJoinDate() != null) {
            user.setJoinDate(request.getJoinDate());
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
    @Caching(
            put = @CachePut(value = CacheNames.USER, key = "#result.id"),
            evict = @CacheEvict(value = CacheNames.USER_PAGE, allEntries = true)
    )
    public UserInfoResponse createUser(UserCreateRequest request) {
        User user = new User();
        assertValidUserName(request.getUserName());
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new BusinessException("Tên người dùng đã được sử dụng, vui lòng chọn tên khác");
        } else {
            user.setUserName(request.getUserName());
        }

        if (userRepository.existsByGmail(request.getGmail())) {
            throw new BusinessException("Gmail này đã được sử dụng");
        } else {
            user.setGmail(request.getGmail());
        }

        if (userRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new BusinessException("Mã số này đã được sử dụng");
        } else {
            user.setStudentNumber(request.getStudentNumber());
        }

        String generatedPassword = generateRandomPassword();

        user.setRole(getRequiredRole(RoleType.valueOf(request.getRoleName())));
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setLocalAuthEnabled(true);
        user.setGoogleLinked(false);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setFullName(request.getFullName());
        user.setVerified(true);
        userRepository.save(user);

        sendPasswordEmail(user.getGmail(), generatedPassword);
        return convertUserInfoToDTO(user);
    }

    @Override
    public PageResponse<UserInfoResponse> searchUser(SearchUserRequest request, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        boolean hasUserName = StringUtils.hasText(request.getUserName());
        boolean hasFullName = StringUtils.hasText(request.getFullName());
        if (hasUserName && hasFullName) {
            Specification<User> identitySpec = UserSpecification.likeUserName(request.getUserName())
                    .or(UserSpecification.likeFullName(request.getFullName()));
            spec = spec.and(identitySpec);
        } else if (hasUserName) {
            spec = spec.and(UserSpecification.likeUserName(request.getUserName()));
        } else if (hasFullName) {
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
                response.getTotalPages(),
                response.getTotalElements(),
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

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private Role getRequiredRole(RoleType roleType) {
        return roleRepository.findFirstByRoleNameOrderByRoleIDAsc(roleType)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleType.name()));
    }

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
            System.err.println("Failed to send password email to " + toGmail + ": " + e.getMessage());
        }
    }

    @Override
    public UserInfoResponse convertUserInfoToDTO(User user) {
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
        userDTO.setActive(user.isActive());
        userDTO.setGoogleLinked(user.isGoogleLinked());
        userDTO.setLocalAuthEnabled(user.isLocalAuthEnabled());
        userDTO.setImageUrl(user.getImageUrl());
        userDTO.setCloudinaryImageId(user.getCloudinaryImageId());
        userDTO.setWorkPlace(user.getWorkPlace());
        userDTO.setJoinDate(user.getJoinDate());
        userDTO.setFieldOfExpertise(user.getFieldOfExpertise());
        userDTO.setBio(user.getBio());
        userDTO.setCreatedDate(user.getCreatedDate());
        return userDTO;
    }

    @Override
    public UserViewResponse convertUserViewToDTO(User user) {
        UserViewResponse userDTO = new UserViewResponse();
        userDTO.setId(user.getId());
        userDTO.setUserName(user.getUserName());
        userDTO.setStudentNumber(user.getStudentNumber());
        userDTO.setFullName(user.getFullName());
        userDTO.setGmail(user.getGmail());
        userDTO.setGoogleLinked(user.isGoogleLinked());
        userDTO.setLocalAuthEnabled(user.isLocalAuthEnabled());
        userDTO.setImageUrl(user.getImageUrl());
        userDTO.setCloudinaryImageId(user.getCloudinaryImageId());
        userDTO.setWorkPlace(user.getWorkPlace());
        userDTO.setJoinDate(user.getJoinDate());
        userDTO.setFieldOfExpertise(user.getFieldOfExpertise());
        userDTO.setBio(user.getBio());
        return userDTO;
    }
}
