package com.example.backend.config;

import com.example.backend.entity.User;
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
        System.out.println("Trying to find user with username: " + username);

        User user;

        if(username.contains("@")){
            user = userService.handleGetUserByGmail(username);
        }
        else{
            user = userService.handleGetUserByUserName(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        if (!user.isVerified()) {
            throw new UnauthorizedException("Chưa xác thực OTP");
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

