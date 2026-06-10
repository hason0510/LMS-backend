package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.exception.AccountLockedException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("userDetailCustom")
public class UserDetailCustom implements UserDetailsService {

    private final UserService userService;

    public UserDetailCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.handleGetUserByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        if (!user.isLocalAuthEnabled() || user.getPassword() == null || user.getPassword().isBlank()) {
            throw new UnauthorizedException("Tai khoan nay chi ho tro dang nhap bang Google");
        }
        if (!user.isVerified()) {
            throw new UnauthorizedException("Chưa xác thực OTP");
        }
        if (!user.isActive()) {
            throw new AccountLockedException();
        }

        String roleName = user.getRole().getRoleName().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleName);

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                List.of(authority)
        );
    }
}

